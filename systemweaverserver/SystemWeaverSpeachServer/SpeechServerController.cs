using System;
using SystemWeaver.Common;
using System.Linq;
using System.Text.RegularExpressions;
using System.Configuration;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Diagnostics;
using SystemWeaver.Common.Internal;
using System.Security.Cryptography;

namespace SpeechServer {
    internal static class SpeechServerController
    {
        private const string EndOfLine = @"<EOL>";
        private static int _levenshteinDistanceLimit;
        internal static NameValueCollection Settings { get; private set; }
        static string[] _sids;
        static string[] _excludedSIDS;

        private static void Main()
        {
            Settings = ConfigurationManager.AppSettings;
            _levenshteinDistanceLimit = int.Parse(Settings["LevenshteinDistanceLimit"]);
            new Server();
            Debugger.Print("Server started");
        }

        private static IswItems GetItems(SpeechClient client) {
            _sids = Settings["SIDS"].Split(';');
            _excludedSIDS = Settings["excludedParts"].Split(';');
            if (_sids.Length == 0) {
                Debugger.Log("NO SIDS SPECIFIED!");
                client.WriteError("NO SIDS SPECIFIED IN CONFIG!");
                return null;
            } else {
                return client.GetChildrenOfTypes(client.CurrentContextNode, _sids, _excludedSIDS);
            }
        }

        internal static bool TryGetPerfectName(string name, SpeechClient client, ref IswItems results)
        {
            Stopwatch s = Stopwatch.StartNew();
            IswItems items = GetItems(client);
            long time = s.ElapsedMilliseconds;
            s.Stop();
            Debugger.Print("RQs:" + time + " Qt: " + items.Count);

            if (items == null || items.Count == 0) {
                results = null;
                return false;
            } 
            string[] names = Regex.Split(name, EndOfLine);

            IswItem perfectMatch = GetPerfectItemMatch(names, items);
            if (perfectMatch != null) {
                results.Add(perfectMatch);
                return true;
            }
            string bestMatch = names[0];

            results = GetBestItemMatches(results, bestMatch, items, int.Parse(Settings["matchesToSend"]));

            return false;
        }

        internal static string GetBestTagMatch(string tagValue, IEnumerable<String> domainValues)
        {
            string[] tagValues = Regex.Split(tagValue, EndOfLine);

            var enumerable = domainValues as IList<string> ?? domainValues.ToList();
            string perfectTag = GetPerfectTagMatch(enumerable, tagValues);
            if (perfectTag != null)
                return perfectTag;

            NLowestScoringStrings lowestScores = new NLowestScoringStrings(_levenshteinDistanceLimit, 1);

            foreach (string domainValue in enumerable)
            {
                int distance = Helper.LevenshteinDistance(tagValues[0], domainValue, lowestScores.Highest);
                lowestScores.TryAdd(new KeyValuePair<string,int>(domainValue,distance));
            }

            string first = lowestScores.GetTopMatches().First().Key;

            return first;
        }

        private static IswItems GetBestItemMatches(IswItems results, string bestMatch, IswItems items, int toTake)
        {
            NLowestScoringItems lowestScores = new NLowestScoringItems(_levenshteinDistanceLimit, toTake);
            foreach (IswItem item in items)
            {
                //int distance = Helper.LevenshteinDistance(item.Name, bestMatch, lowestScores.Highest);
                int distance = Helper.LevenshteinDistance(item.Name, bestMatch);
                lowestScores.TryAdd(new KeyValuePair<IswItem,int>(item,distance));
            }

            var bestMatches = lowestScores.GetTopMatches();
            Debugger.DPrint("Item distances for: " + bestMatch, bestMatches);

            var bestItems = bestMatches.Select(i => i.Key).Take(toTake);

            foreach(IswItem item in bestItems) {
                results.Add(item);
            }
            return results;
        }

        private static string GetPerfectTagMatch(IEnumerable<string> domainValues, IEnumerable<string> tagValues)
        {
            return (from tagValue in tagValues from domainValue in domainValues where tagValue.Equals(domainValue, StringComparison.CurrentCultureIgnoreCase) select domainValue).FirstOrDefault();
        }

        private static IswItem GetPerfectItemMatch(string[] names, IswItems items)
        {
            foreach (string name in names)
                foreach (IswItem item in items)
                {
                    if (item.Name.Equals(name, StringComparison.CurrentCultureIgnoreCase))
                        return item;
                }
            return null;
        }

        internal static string TagItem(IswItem item, string tag, SpeechClient client)
        {
            IswIssue issue = client.Project.AddIssue(client.IssueType, tag);
            issue.Description = SWDescription.PlainTextToDescription(tag);
            issue.AddObjRef(Settings["objreftag"], item);
            issue.SetAttributeWithSID(Settings["attribwithsid"], tag);
            return "Tagged " + item.Name + " with " + tag;
        }

        internal static string TagItemPlusFile(IswItem item, string tag, string filename, byte[] data, SpeechClient client) {
            IswIssue issue = client.Project.AddIssue(client.IssueType, tag);
            issue.Description = SWDescription.PlainTextToDescription(tag);
            issue.AddObjRef(Settings["objreftag"], item);
            issue.SetAttributeWithSID(Settings["attribwithsid"], tag);

//            byte[] compressed = SWCompression.ZCompressBytes(data);

            string hash = String.Empty;
            using (SHA1Managed sha = new SHA1Managed())
            {
                byte[] hashBytes = sha.ComputeHash(data);
                hash = BitConverter.ToString(hashBytes).Replace("-",String.Empty);
            }

            issue.AddFileByData(filename, data, data.Length, hash);

            return "Tagged " + item.Name + " with " + tag + ". and " + filename + " was attached";
        }

        public static string GenerateReport(SpeechClient client, string parameters)
        {
            IswItems items = GetItems(client);

            ReportGenerator repGen = new ReportGenerator(items, client.GetTagValues());
            return repGen.Generate(true,true,true,true);
        }
    }

    class NLowestScoringItems 
    {
        KeyValuePair<IswItem, int>[] topMatches;

        internal int Size { get; private set; }
        internal int Highest { get; private set; }

        internal KeyValuePair<IswItem, int>[] GetTopMatches(){
            return topMatches;
        }
        
        internal NLowestScoringItems(int startValue, int size) {
            Highest = startValue;
            Size = size;
            topMatches = new KeyValuePair<IswItem, int>[size];
            for (int i = 0; i < size; i++)
                topMatches[i] = new KeyValuePair<IswItem, int>(null,startValue);
        }

        internal void TryAdd(KeyValuePair<IswItem, int> itemAndScore) {
            if (itemAndScore.Value < Highest) {
                Add(itemAndScore);
            }
        }

        private void Add(KeyValuePair<IswItem, int> itemAndScore) {
            for (int i = Size - 2; i >= 0; i--) {
                if (itemAndScore.Value >= topMatches[i].Value) {
                    Insert(itemAndScore, i + 1);
                    return;
                }
            }
            Insert(itemAndScore, 0);
        }

        private void Insert(KeyValuePair<IswItem, int> itemAndScore, int index) {
            for (int j = Size-1; j > index; j--) {
                topMatches[j] = topMatches[j - 1];
            }
            topMatches[index] = itemAndScore;
            Highest = topMatches.Last().Value;
        }
    }

    class NLowestScoringStrings {
        KeyValuePair<string, int>[] topMatches;

        internal int Size { get; private set; }
        internal int Highest { get; private set; }

        internal KeyValuePair<string, int>[] GetTopMatches() {
            return topMatches;
        }

        internal NLowestScoringStrings(int startValue, int size) {
            Highest = startValue;
            Size = size;
            topMatches = new KeyValuePair<string, int>[size];
            for (int i = 0; i < size; i++)
                topMatches[i] = new KeyValuePair<string, int>(null, startValue);
        }

        internal void TryAdd(KeyValuePair<string, int> itemAndScore) {
            if (itemAndScore.Value < Highest) {
                Add(itemAndScore);
            }
        }

        private void Add(KeyValuePair<string, int> itemAndScore) {
            for (int i = Size - 2; i >= 0; i--) {
                if (itemAndScore.Value >= topMatches[i].Value) {
                    Insert(itemAndScore, i + 1);
                    return;
                }
            }
            Insert(itemAndScore, 0);
        }

        private void Insert(KeyValuePair<string, int> itemAndScore, int index) {
            for (int j = Size - 1; j > index; j--) {
                topMatches[j] = topMatches[j - 1];
            }
            topMatches[index] = itemAndScore;
            Highest = topMatches.Last().Value;
        }
    }
}