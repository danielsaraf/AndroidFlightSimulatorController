using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public class FlightSimulatorManager : IFlightSimulatorManager
    {
        private readonly IFlightGearClient flightGearClient;
        private readonly SimulatorInfo simInfo;
        private readonly BlockingCollection<AsyncCommand> queue;
        public FlightSimulatorManager(IFlightGearClient client, SimulatorInfo info)
        {
            this.simInfo = info;
            this.flightGearClient = client;
            queue = new BlockingCollection<AsyncCommand>();
            Start();
        }

        /* Get a screenshot from the display in the FlightGear simulator.
         * The function gets the screenshot with httpclient object from
         * the FlightGear web server.
         */
        public async Task<byte[]> GetScreenshotAsync()
        {
            string exceptionMsg = "Something went wrong while attemting get a screenshot";
            var client = new HttpClient
            {
                Timeout = TimeSpan.FromSeconds(10)
            };
            HttpResponseMessage response;
            //getting the response and then extract the screenshot from it.
            try
            {
                response = await client.GetAsync
                ("http://" + simInfo.Ip + ":" + simInfo.HttpPort + "/screenshot");
            }
            catch (Exception)
            {
                throw new Exception(exceptionMsg);
            }
            if (!(response != null && response.IsSuccessStatusCode))
            {
                throw new Exception(exceptionMsg);
            }
            return await response.Content.ReadAsByteArrayAsync();
        }


        /* Verifies that the server gets the a command object with initialized fields.
         */
        private void ValidateCommand(Command command)
        {
            Boolean invalid = false;
            string exceptionMessage = "Invalid Command";
            if (command.Throttle == -10)
                invalid = true;
            if (command.Aileron == -10)
                invalid = true;
            if (command.Elevator == -10)
                invalid = true;
            if (command.Rudder == -10)
                invalid = true;

            if (invalid)
            {
                throw new Exception(exceptionMessage);
            }
        }

        /*
         * Excecute each command in different thread.
         * when the thread finishes the excecutions it let us know
         * by the results enum if the excecution went wrong or ok.
         */
        public Task<Result> Execute(Command cmd)
        {
            try
            {
                ValidateCommand(cmd);
                var asyncCommand = new AsyncCommand(cmd);
                queue.Add(asyncCommand);
                return asyncCommand.Task;
            }
            catch (Exception e)
            {
                throw e;
            }
        }

        private void Start()
        {
            Task.Factory.StartNew(ProccessCommand);
        }

        /*
         * Excecute each command in the queue.
         * If the excecution went wrong it initialize the results with 
         * notok otherwise with ok.
         */
        private void ProccessCommand()
        {
            try
            {
                flightGearClient.Connect(simInfo.Ip, simInfo.TelnetPort);
                this.flightGearClient.Write("data");
            }
            catch (Exception)
            {
                return;
            }

            foreach (AsyncCommand asyncCommnd in queue.GetConsumingEnumerable())
            {
                Result result;
                var command = asyncCommnd.Command;
                try
                {
                    SendCommandAndVerifyAccept(command);
                    result = Result.Ok;
                    asyncCommnd.Completion.SetResult(result);
                }
                catch (Exception)
                {
                    result = Result.NotOk;
                    asyncCommnd.Completion.SetResult(result);
                }
            }
        }

        /*
         * Send the command to the simulator and checks if the command "set" went well.
         * 
         */
        private void SendCommandAndVerifyAccept(Command command)
        {
            string exceptionMessage = "Command send went wrong";
            double flightGearElevatorValue, flightGearAileronValue, flightGearThrottleValue,
                flightGearRudderValue;
            try
            {
                this.flightGearClient.Write("set /controls/flight/rudder " + command.Rudder.ToString());
                this.flightGearClient.Write("set /controls/flight/elevator " + command.Elevator.ToString());
                this.flightGearClient.Write("set /controls/flight/aileron " + command.Aileron.ToString());
                this.flightGearClient.Write("set /controls/engines/current-engine/throttle " +
                    command.Throttle.ToString());

                this.flightGearClient.Write("get /controls/flight/rudder");
                flightGearRudderValue = Convert.ToDouble(this.flightGearClient.Read());
                this.flightGearClient.Write("get /controls/flight/elevator");
                flightGearElevatorValue = Convert.ToDouble(this.flightGearClient.Read());
                this.flightGearClient.Write("get /controls/flight/aileron");
                flightGearAileronValue = Convert.ToDouble(this.flightGearClient.Read());
                this.flightGearClient.Write("get /controls/engines/current-engine/throttle");
                flightGearThrottleValue = Convert.ToDouble(this.flightGearClient.Read());
            }
            catch (Exception e)
            {
                throw e;
            }

            if (Math.Abs(flightGearAileronValue - command.Aileron) > 0.01)
                throw new Exception(exceptionMessage + ": Aileron");
            if (Math.Abs(flightGearRudderValue - command.Rudder) > 0.01)
                throw new Exception(exceptionMessage + ": Rudder");
            if (Math.Abs(flightGearElevatorValue - command.Elevator) > 0.01)
                throw new Exception(exceptionMessage + ": Elevator");
            if (Math.Abs(flightGearThrottleValue - command.Throttle) > 0.01)
                throw new Exception(exceptionMessage + ": Throttle");
        }
    }
}
