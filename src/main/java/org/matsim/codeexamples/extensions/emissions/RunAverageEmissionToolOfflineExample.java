
package org.matsim.codeexamples.extensions.emissions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.contrib.emissions.example.CreateEmissionConfig;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleUtils;

import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.*;


public final class RunAverageEmissionToolOfflineExample{
	private static final Logger log = LogManager.getLogger(RunAverageEmissionToolOfflineExample.class );
	private static final String eventsFile =  "scenarios/sampleScenario/output_events.xml.gz";

	/* package, for test */ static final String emissionEventOutputFileName = "test.emission.events.offline.xml.gz";

	// =======================================================================================================

	public static void main (String[] args){
		// see testcase for an example
		Config config ;
		if ( args==null || args.length==0 || args[0]==null ) {
			config = ConfigUtils.loadConfig( "scenarios/sampleScenario/config_average.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}
		config.plansCalcRoute().clearTeleportedModeParams();

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		{
			// config_average has the emissions config group commented out.  So all that there is to configure (for average emissions)
			// follows here. kai, dec'22

			emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.directlyTryAverageTable );

//		emissionsConfig.setAverageColdEmissionFactorsFile( "../sample_EFA_ColdStart_vehcat_2005average.csv" );
//		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_EFA_HOT_vehcat_2005average.csv" );

			emissionsConfig.setAverageColdEmissionFactorsFile( "sample_EFA_ColdStart_vehcat_2020_average_withHGVetc.csv" );
			emissionsConfig.setAverageWarmEmissionFactorsFile( "sample_41_EFA_HOT_vehcat_2020average.csv" );

			emissionsConfig.setNonScenarioVehicles( NonScenarioVehicles.ignore );

		}
		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// examples for how to set attributes to links and vehicles in order to make this work (already there for example scenario):
		Network network = scenario.getNetwork();
		for (Link link : network.getLinks().values()) {
			// Set HbefaRoadType for each link, adjust as needed based on your data
			EmissionUtils.setHbefaRoadType(link, "YOUR_HBEFA_ROAD_TYPE");
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(EmissionModule.class);
			}
		};
		//AbstractModule module = new AbstractModule(){
		//	@Override
		//	public void install(){
		//		bind( Scenario.class ).toInstance( scenario );
		//		bind( EventsManager.class ).toInstance( eventsManager ) ;
				//bind( EmissionModule.class ) ;
		//	}
	///	};

		com.google.inject.Injector injector = Injector.createInjector( config, module );

		// the EmissionModule must be instantiated, otherwise it does not work:
		injector.getInstance(EmissionModule.class);

		// ---

		// add events writer into emissions event handler
		final EventWriterXML eventWriterXML = new EventWriterXML( config.controler().getOutputDirectory() + '/' + emissionEventOutputFileName );
		eventsManager.addHandler( eventWriterXML );

		// read events file into the events reader.  EmissionsModule and events writer have been added as handlers, and will act accordingly.
		EventsUtils.readEvents( eventsManager, eventsFile );

		// events writer needs to be explicitly closed, otherwise it does not work:
		//eventWriterXML.closeFile();

		// also write vehicles and network and config as a service so we have all out files in one directory:
		new MatsimVehicleWriter( scenario.getVehicles() ).writeFile( config.controler().getOutputDirectory() + "/output_vehicles.xml.gz" );
		NetworkUtils.writeNetwork( scenario.getNetwork(), config.controler().getOutputDirectory() + "/output_network.xml.gz" );
		ConfigUtils.writeConfig( config, config.controler().getOutputDirectory() + "/output_config.xml" );
		ConfigUtils.writeMinimalConfig( config, config.controler().getOutputDirectory() + "/output_config_reduced.xml" );

	}

}
