using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public class Command
    {
        public Command()
        {
            Aileron = -10;
            Rudder = -10;
            Elevator = -10;
            Throttle = -10;
        }

        [JsonProperty, JsonPropertyName("aileron")]
        public double Aileron { get; set; }

        [JsonProperty, JsonPropertyName("rudder")]
        public double Rudder { get; set; }

        [JsonProperty, JsonPropertyName("elevator")]
        public double Elevator { get; set; }

        [JsonProperty, JsonPropertyName("throttle")]
        public double Throttle { get; set; }
    }
}
