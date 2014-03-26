using System;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using SystemWeaver.Common;
using SystemWeaverAPI;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;


namespace SpeechServer
{
    class SpeechClient {
        private const string WaitingForTag = "<WFT>",
                             EndOfLine = "<EOL>",
                             FailedToFindItem = "<FTFI>",
                             ClosestMatchesFound = "<CMF>",
                             ProtocolRootNode = "<ROOT>",
                             ProtocolContextNode = "<CTND>",
                             ProtocolTagInfo = "<INFO>",
                             ProtocolTagError = "<ERROR>";

        private readonly TcpClient _client;
        private readonly ASCIIEncoding _encoder;

        internal byte[] CurrentFileAttachment { get; set; }

        internal IswItems CurrentRootNodes { get; private set; }

        internal IswItems CurrentContextNodes { get; private set; }

        public IswBroker Broker { get; private set; }

        public string ClientAddress { get; private set; }

        internal IswItems CurrentItems { private get; set; }

        public IswIssueType IssueType { get; private set; }

        public IswProject Project { get; private set; }

        private string[] __tagValues;

        public string[] GetTagValues() {
            return __tagValues;
        }

        public void SetTagValues(string[] value) {
            __tagValues = value;
        }

        internal IswItem CurrentItem { get; set; }

        internal IswItem CurrentContextNode { get; set; }


        public SpeechClient(object incomingClient) {
            _client = (TcpClient)incomingClient;
            ClientAddress = ((IPEndPoint)_client.Client.RemoteEndPoint).Address.ToString();
            _client.NoDelay = true;
            _client.Client.NoDelay = true;
            _client.ReceiveTimeout = 9 * 60 * 60 * 1000; //9 hours??
            _encoder = new ASCIIEncoding();
        }

        internal void SendTaggedStrings(IswItems items, string tag) {
            string output = tag;
            for (int i = 0; i < items.Count - 1; i++) {
                output += items[i].Name + EndOfLine;
            }
            output += items[items.Count - 1].Name; //TODO
            Write(output);
        }

        internal void SendClosestMatches(IswItems items) {
            SendTaggedStrings(items, ClosestMatchesFound);
        }

        internal void SendRootNodes() {
            CurrentRootNodes.SortByName();
            SendTaggedStrings(CurrentRootNodes, ProtocolRootNode);
        }

        internal string ReadString() {
            string str = new StreamReader(Read()).ReadToEnd();
            Debugger.Print("Read a string from client");
            Debugger.DPrint("Read string from client: ", str);
            if (String.IsNullOrEmpty(str)) {
                return null;
            }
            //message has successfully been received
            return str;
        }

        internal byte[] ReadByteArray(int filesize) {
            var memoryStream = Read(filesize);
            return memoryStream.ToArray();
        }

        private MemoryStream Read(int filesize) {
            NetworkStream clientStream = _client.GetStream();
            MemoryStream messageStream = new MemoryStream();
            var inbuffer = new byte[filesize];

            if (clientStream.CanRead) {
                int totalBytesRead = 0;
                do {
                    int bytesRead = clientStream.Read(inbuffer, 0, inbuffer.Length);
                    totalBytesRead += bytesRead;
                    messageStream.Write(inbuffer, 0, bytesRead);
                    messageStream.Flush();
                } while (totalBytesRead < filesize);

            }
            messageStream.Position = 0;
            return messageStream;
        }

        /// <summary>
        /// Reads a message from the client
        /// </summary>
        /// <returns>a string with the message or null if nothing to read</returns>
        private MemoryStream Read() {
            NetworkStream clientStream = _client.GetStream();
            MemoryStream messageStream = new MemoryStream();
            var inbuffer = new byte[65535];

            if (clientStream.CanRead) {
                int bytesRead = 0;

                do {
                    bytesRead = clientStream.Read(inbuffer, 0, inbuffer.Length);
                    messageStream.Write(inbuffer, 0, bytesRead);
                    messageStream.Flush();
                    Debugger.Print(messageStream.Length + " added " + bytesRead);
                } while (clientStream.DataAvailable);
            }
            messageStream.Position = 0;
            return messageStream;
        }

        private void Write(string messageToClient) {
            
            //Supposedly \n fixes everything on the client side. Allmighty "\n"
            Debugger.Print("Writing to client");
            Debugger.DPrint("Writing to client:", messageToClient);
            byte[] message = _encoder.GetBytes(messageToClient + "\n");
            NetworkStream clientStream = _client.GetStream();
            clientStream.Write(message, 0, message.Length);
            clientStream.Flush();
        }

        internal IswItem GetItemFromSelection(string name) {
            foreach (IswItem item in CurrentItems) {
                if (item.Name.Equals(name))
                    return item;
            }
            return null;
        }

        internal void Close() {
            _client.Close();
        }

        internal void WriteFailedToFind() {
            Write(FailedToFindItem + "Failed to find item");
        }

        internal void WriteWaitingForTag(string name) {
            Write(WaitingForTag + "What do you want to tag " + name + " with.");
        }

        public void WriteInfo(string message) {
            Write(ProtocolTagInfo + message);
        }

        internal void WriteError(string message) {
            Write(ProtocolTagError + message);
        }

        private void WriteLoggedIn(string username) {
            WriteInfo("Welcome " + username);
        }

        internal bool Connect(string username, string password) {
            SWConnection swCon = SWConnection.Instance;

            swCon.ServerMachineName = SpeechServerController.Settings["ServerMachineName"];
            swCon.ServerPort = int.Parse(SpeechServerController.Settings["ServerPort"]);
            swCon.LoginName = username;
            swCon.Password = password;

            try {
                swCon.Login();
                Broker = swCon.Broker;
                Project = Broker.GetProject(SWHandleUtility.ToHandle(SpeechServerController.Settings["projecthandle"]));
                IssueType = Project.Definition.IssueTypes[1]; //todo settings
                SetTagValues(IssueType.AttributeTypes.FindWithSID(SpeechServerController.Settings["attribwithsid"]).RangeArray);

                Stopwatch s = Stopwatch.StartNew();
                CurrentRootNodes = Broker.Q.GetItemsOfType(SpeechServerController.Settings["RootSID"], true, false);
                long time = s.ElapsedMilliseconds;
                s.Stop();

                Debugger.Print("Proj:" + time + " Qt: " + CurrentRootNodes.Count);

                Debugger.Print("Client logged in" + username);
                WriteLoggedIn(username);
                return true;
            } catch (Exception e) {
                Debugger.Log("Failed to login", e);
                Debugger.Print("Client failed to log in: " + username);
                return false;
            }
        }

        internal void SendContextNodeNames(string rootNode) {
            IswItem selectedRootNode = null;
            foreach (IswItem item in CurrentRootNodes) {
                if (item.Name.Equals(rootNode))
                    selectedRootNode = item;
            }
            if (selectedRootNode == null) {
                Debugger.Log("Client sent bad string");
                WriteError("Sent other string than what was sent to you");
                return;
            }
            string[] sids = SpeechServerController.Settings["ContextSIDS"].Split(';');
            if (sids.Length == 0) {
                Debugger.Log("No contextSids Specified)");
                WriteError("No contextSids Specified)");
                return;
            }
            Stopwatch s = Stopwatch.StartNew();
            CurrentContextNodes = GetContextNodes(selectedRootNode, sids);
            long time = s.ElapsedMilliseconds;
            s.Stop();
            Debugger.Print("Contexts:" +time + " Qt: " + CurrentContextNodes.Count);
            CurrentRootNodes = null;
            SendTaggedStrings(CurrentContextNodes, ProtocolContextNode);
        }

        internal IswItems GetChildrenOfTypes(IswItem root, string[] sids, string[] excluded) {
            IswItems result = Broker.Lists.NewItemList();
            Dictionary<long,byte> visited = new Dictionary<long,byte>();
            GetChildrenOfTypesHelper(root, sids, excluded, result, visited);
            return result;
        }

        private void GetChildrenOfTypesHelper(IswItem root, string[] sids, string[] excluded, IswItems result, Dictionary<long,byte> visited) {
            if(!visited.ContainsKey(root.Handle)){
                visited.Add(root.Handle,0);
                if(root.IsSID(sids)){
                    result.Add(root);
                    return;
                }
                foreach(IswPartType part in root.swItemType.PartTypes) {
                    foreach(string sid in excluded){
                        if (part.SID == sid) {
                            return;
                        }
                    }
                    foreach (IswItem item in root.GetPartItems(part)) {
                        GetChildrenOfTypesHelper(item,sids,excluded,result,visited);
                    }
                }
            }
        }

        internal IswItems GetContextNodes(IswItem root, string[] sids) {
            IswItems result = Broker.Lists.NewItemList();
            foreach (IswPartType part in root.swItemType.PartTypes) {
                foreach (IswItem item in root.GetPartItems(part)) {
                    if (item.IsSID(sids)){
                        result.Add(item);
                    }
                }
            }
            return result;
        }

        internal IswItem GetContextNodeFromSelection(string selection) {
            foreach (IswItem item in CurrentContextNodes) {
                if (item.Name.Equals(selection))
                    return item;
            }
            return null;
        }
    }
}


/*
        /// <summary>
        /// Reads a message from the client
        /// </summary>
        /// <returns>a string with the message or null if nothing to read</returns>
        internal string Read() {
            NetworkStream clientStream = _client.GetStream();

            var message = new byte[4096];

            int bytesRead;
            try {
                //blocks until a client sends a message
                bytesRead = clientStream.Read(message, 0, 4096);
            } catch (Exception e) {
                //a socket error has occured
                Debugger.Print("Socket Error:", e.Message);
                Close();
                return null;
            }

            if (bytesRead == 0) {
                //the client has disconnected from the server
                Close();
                return null;

            }
            Debugger.Print("ReadFromClient");
            Debugger.DPrint("ReadFromClient:", _encoder.GetString(message, 0, bytesRead));
            //message has successfully been received
            return _encoder.GetString(message, 0, bytesRead);
        }
*/