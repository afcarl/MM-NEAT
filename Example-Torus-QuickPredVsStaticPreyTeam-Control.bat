java -jar dist/MM-NEATv2.jar runNumber:%1 randomSeed:%1 base:torus trials:20 maxGens:1000 mu:100 io:true netio:true mating:false fs:false task:edu.utexas.cs.nn.tasks.gridTorus.TorusEvolvedPredatorsVsStaticPreyTask log:QuickPredVsStaticPreyTeam-Control saveTo:Control allowDoNothingActionForPredators:true torusPreys:2 torusPredators:3 staticPreyController:edu.utexas.cs.nn.gridTorus.controllers.PreyFleeClosestPredatorController predatorMinimizeTotalTime:false predatorsEatQuick:true torusSenseTeammates:true