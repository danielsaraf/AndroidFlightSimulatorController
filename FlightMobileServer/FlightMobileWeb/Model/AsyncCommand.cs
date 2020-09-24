using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace FlightMobileWeb.Model
{
    public enum Result { Ok, NotOk }
    public class AsyncCommand
    {
        public AsyncCommand(Command input)
        {
            Command = input;
            Completion = new TaskCompletionSource<Result>(
                TaskCreationOptions.RunContinuationsAsynchronously);
        }

        public Command Command { get; set; }

        public TaskCompletionSource<Result> Completion { get; private set; }
        public Task<Result> Task { get => Completion.Task; }
    }
}
