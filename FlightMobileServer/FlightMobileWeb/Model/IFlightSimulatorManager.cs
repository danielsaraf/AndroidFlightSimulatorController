using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public interface IFlightSimulatorManager
    {
        Task<byte[]> GetScreenshotAsync();

        Task<Result> Execute(Command cmd);
    }
}
