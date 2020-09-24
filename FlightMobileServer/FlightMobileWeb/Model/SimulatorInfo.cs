using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public class SimulatorInfo
    {
        public SimulatorInfo(string ip, string httpPort, string telnetPort)
        {
            Ip = ip;
            TelnetPort = telnetPort;
            HttpPort = httpPort;
        }
        public string Ip { get; set; }

        public string TelnetPort { get; set; }

        public string HttpPort { get; set; }

    }
}
