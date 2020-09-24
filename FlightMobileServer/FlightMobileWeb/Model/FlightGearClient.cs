using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public class FlightGearClient : IFlightGearClient
    {
        private readonly TcpClient myClient;
        private NetworkStream netStream;
        private readonly string defaultIp = "127.0.0.1";
        private readonly string defaultPort = "5402";

        public FlightGearClient()
        {
            this.myClient = new TcpClient();
            myClient.ReceiveTimeout = 10000;
        }
        public bool IsConnected()
        {
            return myClient.Connected;
        }
        //try to connect with the default ip and port.
        public void Connect()
        {
            try
            {
                Connect(defaultIp, defaultPort);
            }
            catch (Exception e)
            {
                throw e;
            }
        }
        public void Connect(string ip, string port)
        {
            // try to connect the server
            try
            {

                this.myClient.Connect(ip, int.Parse(port));
                netStream = myClient.GetStream();
            }
            catch (Exception)
            {
                throw new Exception("Cannot connect to FlightGear Simulator");
            }
        }

        /*
         * Write to the simulator the command and add \r\n to it.
         */
        public void Write(string command)
        {
            try
            {
                byte[] messageSent = Encoding.ASCII.GetBytes(command + "\r\n");
                netStream.Write(messageSent, 0, messageSent.Length);
            }
            catch (Exception)
            {
                throw new Exception("There was a problem while trying to write to the Simulator");
            }

        }

        /*
         * Read the bytes that the simulator send us after "get" command.
         */
        public string Read()
        {
            try
            {
                byte[] bytes = new byte[myClient.ReceiveBufferSize];
                netStream.Read(bytes, 0, (int)myClient.ReceiveBufferSize);
                return Encoding.UTF8.GetString(bytes);
            }
            catch (Exception)
            {
                throw new Exception
                    ("There was a problem while trying to recieve data from the Simulator");
            }
        }

        public void Disconnect()
        {
            //disconnect from server
            try
            {
                netStream.Close();
                myClient.Close();
            }
            catch (Exception)
            {
                throw new Exception
                    ("There was a problem while trying to disconnect from the simulator");
            }
        }
    }
}
