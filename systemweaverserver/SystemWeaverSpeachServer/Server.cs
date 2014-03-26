using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text.RegularExpressions;
using System.Threading;
using SystemWeaver.Common;



namespace SpeechServer {
    /// <summary>
    /// The server that handles client communication
    /// </summary>
    internal class Server
    {

        private const string ProtocolId = "<ID>",
                             ProtocolSelection = "<SEL>",
                             ProtocolTag = "<TAG>",
                             ProtocolTagPlus = "<TAGP>",
                             ProtocolFile = "<FILE>",
                             ProtocolFileSize = "<FSIZE>",
                             ProtocolReportGeneration = "<RPGN>",
                             ProtocolRootNode = "<ROOT>",
                             ProtocolContextNode = "<CTND>",
                             ProtocolUsernamePassword = "<LOGIN>";


        private readonly TcpListener _tcpListener;
        private readonly Thread _listenThread;

        public Server() {
            int serverSocket = int.Parse(SpeechServerController.Settings["SpeechServerSocket"]);
            _tcpListener = new TcpListener(IPAddress.Any, serverSocket);
            _listenThread = new Thread(ListenForClients) { Name = "ServerListenThread" };
            _listenThread.Start();
        }

        /// <summary>
        /// listens for clients
        /// </summary>
        private void ListenForClients() {
            _tcpListener.Start();

            while (true) {
                //Blocks until a client has connected to the server
                TcpClient client = _tcpListener.AcceptTcpClient();

                //create a thread to handle communication with connected client
                var clientThread = new Thread(HandleClientComm)
                    {
                        Name = "ClientThread: " + ((IPEndPoint) client.Client.RemoteEndPoint).Address
                    };
                clientThread.Start(client);
            }
        }

        /// <summary>
        /// handles the clinent communication
        /// </summary>
        /// <param name="client">the client that's beeing handled</param>
        private static void HandleClientComm(object client) {
            var speechClient = new SpeechClient(client);

            //try {
                HandleMessages(speechClient);
            // } catch (Exception e) {
            //     Debugger.Log("Exception in handle message", e);
            //     Debugger.Print("Exception in handle message", e.StackTrace);
            //}
            Debugger.Print("Disconnected:",speechClient.ClientAddress);
            speechClient.Close();
        }

        /// <summary>
        /// Handles messages from the client
        /// </summary>
        /// <param name="client">the client to handle</param>
        private static void HandleMessages(SpeechClient client)
        {
            if (!LogInClient(client))
                return;
            client.SendRootNodes();

            string read = client.ReadString();
            while (read != null)
            {
                if (read.StartsWith(ProtocolId))
                {
                    string name = read.Replace(ProtocolId, String.Empty);
                    HandleId(client, name);
                }
                else if (read.StartsWith(ProtocolSelection))
                {
                    string selection = read.Replace(ProtocolSelection, String.Empty);
                    HandleSelection(client, selection);
                } 
                else if (read.StartsWith(ProtocolTag))
                {
                    string tag = read.Replace(ProtocolTag, String.Empty);
                    HandleTag(client, tag);
                } else if (read.StartsWith(ProtocolTagPlus)) 
                {
                    string tag = read.Replace(ProtocolTagPlus, String.Empty);
                    string filename = String.Empty;
                    int filesize = 0;

                    read = client.ReadString();
                    if (read.StartsWith(ProtocolFile))
                    {
                        string fileinfo = read.Replace(ProtocolFile, String.Empty);
                        string[] filenameNfilesize = Regex.Split(fileinfo, ProtocolFileSize);
                        filename = filenameNfilesize[0];
                        filesize = int.Parse(filenameNfilesize[1]);
                        HandleTagPlus(client, tag, filename, filesize);
                    }
                    else {
                        client.WriteError("No file info after tagplus");
                    }
                }
                else if (read.StartsWith(ProtocolReportGeneration))
                {
                    string parameters = read.Replace(ProtocolTag, String.Empty);
                    string message = SpeechServerController.GenerateReport(client, parameters);
                    client.WriteInfo(message);
                }
                else if (read.StartsWith(ProtocolRootNode)) {
                    string rootNode = read.Replace(ProtocolRootNode, String.Empty);
                    HandleRootNode(client, rootNode);
                }
                else if (read.StartsWith(ProtocolContextNode)) {
                    string contextNode = read.Replace(ProtocolContextNode, String.Empty);
                    HandleContextNode(client, contextNode);
                }
                    /*
                else if (read.StartsWith("<FILE>")) {
                    string fileNsize = read.Replace("<FILE>", String.Empty);
                    string[] tagNFilename = Regex.Split(fileNsize, "<FSIZE>");
                    string filename = tagNFilename[0];
                    int filesize = int.Parse(tagNFilename[1]);
                    HandleFile(client, filename, filesize);
                }
                     * */
                else {
                    Debugger.Log("Undefined protocol inputTag: " + read);
                    client.WriteError("Undefined protocol inputTag: " + read);
                }
                read = client.ReadString();
            }
        }

        private static void HandleRootNode(SpeechClient client, string rootNode) {
            client.SendContextNodeNames(rootNode);
            client.CurrentItem = null;
            client.CurrentItems = null;
            client.CurrentFileAttachment = null;
            client.CurrentContextNode = null;
        }

        private static void HandleContextNode(SpeechClient client, string selection) {
            client.CurrentContextNode = client.GetContextNodeFromSelection(selection);
            client.WriteInfo("Context selected");
            client.CurrentItem = null;
            client.CurrentItems = null;
            client.CurrentFileAttachment = null;
        }



        private static bool LogInClient(SpeechClient client)
        {
            string input = client.ReadString();
            if (input != null && input.Contains(ProtocolUsernamePassword))
            {
                string[] both = Regex.Split(input, ProtocolUsernamePassword);
                string username = both[0];
                string password = both[1];
                return client.Connect(username,password);
            }
            return false;
        }

        private static void HandleSelection(SpeechClient client, string selection) {
            client.CurrentItem = client.GetItemFromSelection(selection);
            client.CurrentItems = null;
            client.CurrentFileAttachment = null;
            client.WriteWaitingForTag(client.CurrentItem.Name);
        }

        private static void HandleId(SpeechClient client, string name) {
            IswItems items = client.Broker.Lists.NewItemList();
            bool perfectMatch = SpeechServerController.TryGetPerfectName(name, client, ref items);
            if (perfectMatch)
            {
                client.CurrentItems = null;
                client.CurrentFileAttachment = null;
                client.CurrentItem = items[0];
                client.WriteWaitingForTag(client.CurrentItem.Name);
            }
            else if (items == null || items.Count == 0)
            {
                client.CurrentItems = null;
                client.CurrentItem = null;
                client.CurrentFileAttachment = null;
                client.WriteFailedToFind();
            }
            else
            {
                client.CurrentItems = items;
                client.CurrentItem = null;
                client.CurrentFileAttachment = null;
                client.SendClosestMatches(items);
            }
        }

        private static void HandleTag(SpeechClient client, string inputTag) {
            string[] tagValues = client.GetTagValues();
            string tag = SpeechServerController.GetBestTagMatch(inputTag, tagValues);
            if (tag == null)
            {
                client.WriteError("No tags matching"); //todo cannot happen?
                return;
            }
            string message = SpeechServerController.TagItem(client.CurrentItem, tag, client);
            client.WriteInfo(message);
            client.CurrentItem = null;
            client.CurrentItems = null;
        }

        private static void HandleTagPlus(SpeechClient client, string inputTag, string filename, int filesize) {
            string[] tagValues = client.GetTagValues();
            string tag = SpeechServerController.GetBestTagMatch(inputTag, tagValues);
            if (tag == null) {
                client.WriteError("No tags matching"); //todo cannot happen?
                return;
            }

            byte[] fileData = client.ReadByteArray(filesize);
            File.WriteAllBytes(@"C:\" + filename, fileData);

            string message = SpeechServerController.TagItemPlusFile(client.CurrentItem, tag, filename, fileData, client);
            

            client.WriteInfo(message);
            client.CurrentItem = null;
            client.CurrentItems = null;
        }

            private static void HandleFile(SpeechClient client, string filename, int filesize) {
                
                var bytearr = client.ReadByteArray(filesize);
                File.WriteAllBytes(@"C:\" + filename, bytearr);
        }
    }
}