using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public interface IFlightGearClient
    {
        void Connect(string ip, string port);
        bool IsConnected();

        void Write(string command);

        string Read();

        void Disconnect();

        void Connect();
    }
}
