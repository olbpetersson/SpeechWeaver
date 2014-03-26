
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using SystemWeaver.Common;
using System.Globalization;

namespace SpeechServer {

    internal static class Debugger
    {
        private const string EndOfLine = @"<EOL>";

        public static void Print(string message)
        {
            Console.WriteLine(DateTime.Now.ToString("HH.mm", CultureInfo.CurrentCulture) + ": " + message);
        }

        public static void Print(string header, string message) {
            if (message.Contains(EndOfLine))
            {
                StringBuilder sb = new StringBuilder();
                sb.Append(DateTime.Now.ToString("HH.mm", CultureInfo.CurrentCulture)).Append(": ").Append(header); 
                string[] lines = Regex.Split(message, EndOfLine);
                foreach (String line in lines)
                {
                    sb.AppendLine(line);
                }
                Console.WriteLine(sb.ToString());
            }
            else
            {
                Console.WriteLine(DateTime.Now.ToString("HH.mm", CultureInfo.CurrentCulture) +": " + header + "\n" + message);
            }
        }

        internal static void DPrint(string header, IEnumerable<KeyValuePair<IswItem, int>> list)
        {
            if (Boolean.Parse(SpeechServerController.Settings["debug"])) {
                StringBuilder message = new StringBuilder();
                foreach (KeyValuePair<IswItem, int> kv in list) {
                    message.Append(kv.Value).Append(" -| ").Append(kv.Key.Name).AppendLine();
                }
                Print(header, message.ToString());
            }
        }

        internal static void DPrint(string header, IEnumerable<KeyValuePair<string, int>> list)
        {
            if (Boolean.Parse(SpeechServerController.Settings["debug"])) {
                StringBuilder message = new StringBuilder();
                foreach (KeyValuePair<string, int> kv in list) {
                    message.Append(kv.Value).Append(" -| ").Append(kv.Key).AppendLine();
                }
                Print(header, message.ToString());
            }
        }

        public static void Log(string output, Exception e) {
            Log(output + "\nException: " + e.GetType() + "\n" + "Message: " + e.Message + "\n" + "Stacktrace: " + e.StackTrace);
        }

        public static void Log(string output) {
            if(Boolean.Parse(SpeechServerController.Settings["debug"])) {
                            Console.Out.WriteLine(output);
            }
            using (var file = new StreamWriter(SpeechServerController.Settings["LogFile"], true)) {
                file.WriteLine(DateTime.Now.ToString("HH:mm:ss", CultureInfo.CurrentCulture) + " : " + output);
            }
        }

        public static void DPrint(string message) {
            if (Boolean.Parse(SpeechServerController.Settings["debug"]))
                Print(message);
        }

        public static void DPrint(string header, string message) {
            if (Boolean.Parse(SpeechServerController.Settings["debug"]))
                Print(header, message);
        }
    }
}