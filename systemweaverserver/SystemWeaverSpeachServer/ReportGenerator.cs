using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Windows.Forms;
using SystemWeaver.Common;
using System.Windows.Forms.DataVisualization.Charting;
using System.Text;
using System.Globalization;

namespace SpeechServer
{
    internal class ReportGenerator
    {
        readonly Dictionary<IswItem, List<IswIssue>> _reqDict = new Dictionary<IswItem, List<IswIssue>>();
        readonly Dictionary<string, List<IswItem>> _annotDict = new Dictionary<string, List<IswItem>>();
        private readonly Dictionary<string, List<IswItem>> _noDuplicateAnnotDict = new Dictionary<string, List<IswItem>>();

        private readonly int _numberOfReqs;
        private readonly int _numberOfAnnotations;
        

        /// <summary>
        /// Creates a report structure
        /// </summary>
        /// <param name="items">all items to be included in the report</param>
        /// <param name="annotationValues">all possible annotations</param>
        public ReportGenerator(IswItems items, IEnumerable<string> annotationValues)
        {
            //Req to issue
            _numberOfReqs = items.Count();

            foreach (IswItem item in items)
            {
                IswIssueObjRefs swIssueObjRefs = item.GetIssueRefs(SpeechServerController.Settings["objreftag"]);
                if (swIssueObjRefs.Count == 0)
                    continue;


                
                List<IswIssue> reqList;

                if (!_reqDict.TryGetValue(item, out reqList))
                    reqList = new List<IswIssue>();

                reqList.AddRange(from IswIssueObjRef swIssueObjRef in swIssueObjRefs select swIssueObjRef.Issue);
                _reqDict.Add(item, reqList);
            }

            //issue to req
            foreach (string tagValue in annotationValues)
            {
                _annotDict.Add(tagValue,new List<IswItem>());
            }

            foreach (IswItem item in _reqDict.Keys)
            {
                foreach (var issue in _reqDict[item])
                {
                    var listOfReq = _annotDict[issue.FindAttributeWithSID(SpeechServerController.Settings["attribwithsid"]).ValueAsString];
                    listOfReq.Add(item);
                }
            }
            _numberOfAnnotations = _annotDict.Keys.Sum(annotation => _annotDict[annotation].Count);
            _noDuplicateAnnotDict = _annotDict.Keys.ToDictionary(annotation => annotation, annotation => _annotDict[annotation].Distinct().ToList());

        }

        /// <summary>
        /// Geberates the report
        /// </summary>
        /// <param name="parameters">To be defined...</param>
        /// <returns>Message of what happened</returns>
        public string Generate(params bool[] parameters)
        {
            string time = DateTime.Now.ToString("HH mm", CultureInfo.CurrentCulture);
            
            //using (var file = new StreamWriter(SpeechServerController.Settings["ReportLocation"] + "Report" + time.Replace(" ",String.Empty) + ".txt" + , false))
            using (var file = new StreamWriter(SpeechServerController.Settings["ReportLocation"] + "Report.txt", false))
            {
                if (parameters[0])
                {
                    PrintRequirement(file);
                }
                if (parameters[1]) {
                    PrintRequirementSummary(file);
                    MakeRequirementSummaryDiagram();
                }
                if (parameters[2]) {
                    PrintAnnotationSummary(file);
                    MakeAnnotationSummaryDiagram();

                }
                if (parameters[3]) {
                    PrintAnnotation(file);
                }
            }
            return "The time is: " + time + ". A report was generated!";
        }

        private void MakeAnnotationSummaryDiagram()
        {
            Dictionary<string, int> dataToPlot = _annotDict.Keys.ToDictionary(annotation => annotation, annotation => _annotDict[annotation].Count);
            MakeDiagram(dataToPlot, "annotationSummary",false);
        }

        private void MakeRequirementSummaryDiagram()
        {
            Dictionary<string, int> dataToPlot = _noDuplicateAnnotDict.Keys.ToDictionary(annotation => annotation, annotation => _noDuplicateAnnotDict[annotation].Count);
            MakeDiagram(dataToPlot, "requirementSummary",false);
        }

        public static void MakeDiagram(Dictionary<string, int> dataToPlot, string fileName, bool doughnut)
        {
            using (var chart = new Chart())
            {
                chart.ChartAreas.Add(new ChartArea());
                chart.Legends.Add("People");
                var s = new Series("Data");
                foreach (string name in dataToPlot.Keys)
                {
                    s.Points.AddXY(name, dataToPlot[name]);
                }
                chart.Series.Add(s);

                chart.Series["Data"].ChartType = doughnut ? SeriesChartType.Doughnut : SeriesChartType.Pie;
                chart.Series["Data"]["PieLabelStyle"] = "Outside";
                chart.Series["Data"]["PieLineColor"] = "Black";
                chart.Series["Data"].Legend = "People";
                chart.Series["Data"].LegendText = "#PERCENT{P2}";
                chart.Series["Data"].BorderColor = System.Drawing.Color.FromArgb(0, 0, 0);
                chart.Series["Data"].BorderWidth = 2;

                chart.Legends["People"].Docking = Docking.Bottom;
                chart.Legends["People"].Enabled = true;
                chart.Legends["People"].Alignment = System.Drawing.StringAlignment.Center;

                chart.Dock = DockStyle.None; //??

                chart.AntiAliasing = AntiAliasingStyles.Graphics;

                chart.Height = 700;
                chart.Width = 700;

                chart.DataManipulator.Sort(PointSortOrder.Descending, chart.Series["Data"]);

                chart.SaveImage(SpeechServerController.Settings["SummaryImageLocation"] + fileName + ".png", ChartImageFormat.Png);
            }
        }


        private void PrintAnnotationSummary(StreamWriter file) {
            PrintSummary("ANNOTATION SUMMARY", file, _annotDict, _numberOfAnnotations);
        }

        private void PrintRequirementSummary(StreamWriter file)
        {
            PrintSummary("REQUIREMENT SUMMARY", file, _noDuplicateAnnotDict, _numberOfReqs);
        }

        private void PrintSummary(string title, StreamWriter file, Dictionary<string, List<IswItem>> dict, int total)
        {
            string pretty = new string('=', title.Length);
            StringBuilder output = new StringBuilder(pretty);
            output.AppendLine().Append(title).AppendLine();
            output.Append(pretty);

            foreach (var annotation in dict.Keys) {
                int nrOfAnnotations = dict[annotation].Count;
                double quota = 100 * nrOfAnnotations / (double)total;
                output.AppendLine().Append(annotation).Append(": \t");
                output.Append(nrOfAnnotations).Append(" (").Append(Math.Round(quota, 2) + "%)");
            }
            output.AppendLine().Append("Total: \t").Append(total);
            Debugger.DPrint(output.ToString());
            file.WriteLine(output.ToString());
        }

        private void PrintRequirement(StreamWriter file)
        {
            const string title = "REQUIREMENTS";
            string pretty = new string('=', title.Length);
            StringBuilder output = new StringBuilder(pretty);
            output.AppendLine().Append(title).AppendLine();
            output.Append(new string('=', title.Length));

            foreach (IswItem item in _reqDict.Keys)
            {
                output.AppendLine().Append(item.Name);
                foreach (IswIssue annotation in _reqDict[item])
                {
                    output.AppendLine().Append(annotation.FindAttributeWithSID(SpeechServerController.Settings["attribwithsid"]).ValueAsString);
                }
                output.Append("\n--------------");
            }
            Debugger.DPrint(output.ToString());
            file.WriteLine(output.ToString());
        }

        private void PrintAnnotation(StreamWriter file)
        {
            const string title = "ANNOTATIONS";
            string pretty = new string('=', title.Length);
            StringBuilder output = new StringBuilder(pretty);

            output.AppendLine().Append(title).AppendLine();
            output.Append(pretty);

            foreach (string annotation in _annotDict.Keys)
            {
                output.AppendLine().Append(annotation);
                var grp = _annotDict[annotation].GroupBy(i => i);
                foreach (var item in grp)
                {
                    output.AppendLine().Append(String.Format(CultureInfo.CurrentCulture, "{0} - {1}", item.Key.Name, item.Count()));
                }
                output.AppendLine().Append("---------");
            }
            Debugger.DPrint(output.ToString());
            file.WriteLine(output.ToString());
        }
    }
}