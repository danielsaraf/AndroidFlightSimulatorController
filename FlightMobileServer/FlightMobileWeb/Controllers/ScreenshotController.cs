using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using System.Xml.Linq;
using FlightMobileWeb.Model;
using Microsoft.AspNetCore.Mvc;

namespace FlightMobileWeb.Controllers
{
    [Route("[controller]")]
    [ApiController]
    public class ScreenshotController : ControllerBase
    {
        private readonly IFlightSimulatorManager flightManager;
        public ScreenshotController(IFlightSimulatorManager commandSender)
        {
            flightManager = commandSender;
        }
        // GET: /Screenshot
        /* The Function returns the screenshot from the FlightGearSimulator display.
         * If an exception has occured then the function return BadRequest.
         * 
         */
        [HttpGet]
        public async Task<IActionResult> GetScreenshotAsync()
        {
            byte[] img;
            try
            {
                img = await flightManager.GetScreenshotAsync();
            }
            catch (Exception e)
            {
                return NotFound(e.Message);
            }
            return File(img, "Image/jpg");
        }
    }
}
