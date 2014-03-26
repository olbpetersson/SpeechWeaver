using System;

namespace SpeechServer {

    internal static class Helper {

        /// <summary>
        /// Compute the distance between two strings.
        ///
        /// @@The following is a code sample implementing Dr Levensthein distance algorithm.
        /// Taken from http://www.dotnetperls.com/levenshtein
        /// </summary>
        public static int LevenshteinDistance(string s, string t, int limit) {
            int n = s.Length;
            int m = t.Length;
            var d = new int[n + 1, m + 1];

            // Step 1
            if (n == 0) {
                return m;
            }

            if (m == 0) {
                return n;
            }

            // Step 2
            for (int i = 0; i <= n; d[i, 0] = i++) {
            }

            for (int j = 0; j <= m; d[0, j] = j++) {
            }

            // Step 3
            for (int i = 1; i <= n; i++) {

                //Step 4
                for (int j = 1; j <= m; j++) {

                    // Step 5
                    int cost = (t[j - 1] == s[i - 1]) ? 0 : 1;

                    // Step 6
                    int costSoFar = Math.Min(
                        Math.Min(d[i - 1, j] + 1, d[i, j - 1] + 1),
                        d[i - 1, j - 1] + cost);
                    if (costSoFar >= limit)
                        return int.MaxValue;
                    else 
                        d[i, j] = costSoFar;
                }
            }

            // Step 7
            return d[n, m];
        }

        public static int LevenshteinDistance(string s, string t) {
            int n = s.Length;
            int m = t.Length;
            var d = new int[n + 1, m + 1];

            // Step 1
            if (n == 0) {
                return m;
            }

            if (m == 0) {
                return n;
            }

            // Step 2
            for (int i = 0; i <= n; d[i, 0] = i++) {
            }

            for (int j = 0; j <= m; d[0, j] = j++) {
            }

            // Step 3
            for (int i = 1; i <= n; i++) {

                //Step 4
                for (int j = 1; j <= m; j++) {

                    // Step 5
                    int cost = (t[j - 1] == s[i - 1]) ? 0 : 1;

                    // Step 6
                    int costSoFar = Math.Min(
                        Math.Min(d[i - 1, j] + 1, d[i, j - 1] + 1),
                        d[i - 1, j - 1] + cost);
                    d[i, j] = costSoFar;
                }
            }

            // Step 7
            return d[n, m];
        }
    }
}