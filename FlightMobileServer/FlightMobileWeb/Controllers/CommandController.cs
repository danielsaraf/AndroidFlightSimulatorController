using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using FlightMobileWeb.Model;


namespace FlightMobileWeb.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class CommandController : ControllerBase
    {
        readonly IFlightSimulatorManager FlightManager;
        public CommandController(IFlightSimulatorManager commandSender)
        {
            FlightManager = commandSender;
        }

        // post: api/Command
        /* the function try to send the command to the simulator by 
         * the FlightManager object.
         *  If an exception has occured or the result is not ok, the
         *  function returns BadRequest.
         */
        [HttpPost]
        public IActionResult Post([FromBody] Command value)
        {
            Result result;
            try
            {
                //Gets the results from the flightmanager
                result = FlightManager.Execute(value).Result;

            }
            catch (Exception e)
            {
                return BadRequest(e.Message);
            }

            if (result == Result.NotOk)
            {
                return BadRequest("Something" +
                    " went wrong while sending the command to the simulator");
            }
            return Ok();
        }
    }
}
