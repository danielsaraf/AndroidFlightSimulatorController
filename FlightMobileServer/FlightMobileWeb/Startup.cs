using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using FlightMobileWeb.Model;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace FlightMobileWeb
{
    public class Startup
    {
        public Startup(IConfiguration configuration)
        {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services)
        {
            string ip = Configuration.GetValue<string>("SimulatorInfo:IP");
            string httpPort = Configuration.GetValue<string>("SimulatorInfo:HttpPort");
            string telnetPort = Configuration.GetValue<string>("SimulatorInfo:TelnetPort");
            SimulatorInfo info = new SimulatorInfo(ip, httpPort, telnetPort);
            services.AddControllers();
            services.AddSingleton(info);
            services.AddSingleton(typeof(IFlightGearClient), typeof(FlightGearClient));
            services.AddSingleton(typeof(IFlightSimulatorManager), typeof(FlightSimulatorManager));
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseRouting();

            app.UseAuthorization();

            app.UseEndpoints(endpoints =>
            {
                endpoints.MapControllers();
            });
        }
    }
}
