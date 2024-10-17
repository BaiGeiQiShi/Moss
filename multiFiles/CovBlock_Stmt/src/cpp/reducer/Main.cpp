#include <string>

#include <spdlog/spdlog.h>
#include <spdlog/sinks/stdout_sinks.h>
#include <spdlog/sinks/basic_file_sink.h>
#include <stdlib.h>
#include <time.h>

#include "DeadcodeElimination.h"
#include "FileManager.h"
#include "Frontend.h"
#include "IntegrationManager.h"
#include "LocalReduction.h"
#include "ASTsList.h"
#include "OptionManager.h"
#include "Profiler.h"
#include "Reduction.h"
#include "Reformat.h"
#include "Report.h"
#include "StatsManager.h"


void initialize()
{
	FileManager::Initialize();
	llvm::errs()<<OptionManager::OutputDir<<"\n";
	auto SimplifiedFileSink = std::make_shared<spdlog::sinks::basic_file_sink_mt>(
		OptionManager::OutputDir + "/Simplifiedlog.txt", true);
	SimplifiedFileSink->set_pattern("%v");
	auto SLogger = std::make_shared<spdlog::logger>("SLogger",SimplifiedFileSink);
	SLogger->set_level(spdlog::level::info);
	SLogger->flush_on(spdlog::level::info);
	spdlog::register_logger(SLogger);
	auto ConsolSink = std::make_shared<spdlog::sinks::stdout_sink_mt>();
	auto FileSink = std::make_shared<spdlog::sinks::basic_file_sink_mt>(
		OptionManager::OutputDir + "/log.txt", true);

	ConsolSink->set_pattern("%v");
	auto Logger = std::make_shared<spdlog::logger>(
		"Logger", spdlog::sinks_init_list{ConsolSink, FileSink});
	Logger->flush_on(spdlog::level::trace);
	if (OptionManager::Debug)
	{
		ConsolSink->set_level(spdlog::level::debug);
		FileSink->set_level(spdlog::level::debug);
		Logger->set_level(spdlog::level::debug);
	}
	else
	{
		ConsolSink->set_level(spdlog::level::info);
		FileSink->set_level(spdlog::level::info);
		Logger->set_level(spdlog::level::info);
	}
	spdlog::register_logger(Logger);

	IntegrationManager::Initialize();
	Profiler::Initialize();
	spdlog::get("Logger")->info("Oracle: {}", OptionManager::OracleFile);
	for (auto &File : OptionManager::InputFiles)
		spdlog::get("Logger")->info("Input: {}", File);
	spdlog::get("Logger")->info("Output Directory: {}", OptionManager::OutputDir);
	ASTsList::Initialize();
}

void finalize()
{
	IntegrationManager::Finalize();
	FileManager::Finalize();
	Profiler::Finalize();
}

int reduceOneFile(std::string &File)
{
	OptionManager::InputFile = File;

	StatsManager::ComputeStats(OptionManager::InputFile);

	/// Reduce on origin.c, to make sure localreduction can get all program-elements.
	///  The localreduction will knock out all the statements of the complement set of `Origin` and `File` at *first*(Sync),
	///  and then do the furthur add-back/remove work.
	std::string Origin = File + ".origin.c";
	llvm::sys::fs::copy_file(Origin, File);

	Reduction *LR = new LocalReduction();
	Frontend::Parse(OptionManager::InputFile, LR);

	return 0;
}

void reduceProject()
{
	spdlog::get("Logger")->info("*Start Reduction on the Project");

	Profiler::Init();
	FileManager::GetInstance()->saveSample("sample-1"); // Save the initial program
	
	int curr_sample = 0;
	int curr_iter = 0;
	while (curr_sample < OptionManager::MaxSamples && curr_iter < OptionManager::MaxIters)
	{
		std::get<0>(Profiler::curr_iter_info) = curr_iter;
		std::get<1>(Profiler::curr_iter_info) = curr_sample;

		bool revert_all = false;
		spdlog::get("Logger")->info("");
		spdlog::get("Logger")->info("Current Iteration: {}; Current Samples: {}", curr_iter, curr_sample);
		curr_iter += 1;

		ASTsList::SelectElemIdx();
		spdlog::get("Logger")->info("-Select Element Index {}", ASTsList::SelectingElementIdx);
		spdlog::get("Logger")->info("- {}", ASTsList::AddbackOrRemoveFlag ? "REMOVE" : "ADDBACK");
		for (auto reducefile : OptionManager::InputFiles)
		{
			reduceOneFile(reducefile);
		}

		Profiler::EvalReducedProject();

		if (std::get<8>(Profiler::curr_iter_info) == false)
		{
			revert_all = true;
		}
		else if (std::get<4>(Profiler::curr_iter_info) < 0 || // R
				 std::get<5>(Profiler::curr_iter_info) < 0 || // G
				 std::get<6>(Profiler::curr_iter_info) < 0)	  // O
		{
			spdlog::get("Logger")->info("Getting Evaluation Result Failure. Revert");
			revert_all = true;
		}

		if (revert_all)
		{				
			spdlog::get("Logger")->info("Accepted? Reject");
			spdlog::get("Logger")->info("====Revert====");
			for (auto reducefile : OptionManager::InputFiles)
			{
				reduceOneFile(reducefile);
			}
		}
		else // can compile. Next step: check scores
		{
			spdlog::get("Logger")->info("Sample Id: {}", std::to_string(curr_sample));
			spdlog::get("Logger")->info("SR-Score: {}; AR-Score: {}; R-Score: {}; G-Score: {}; O-Score: {}; D-Score: {}",
										std::get<2>(Profiler::curr_iter_info), std::get<3>(Profiler::curr_iter_info),
										std::get<4>(Profiler::curr_iter_info), std::get<5>(Profiler::curr_iter_info),
										std::get<6>(Profiler::curr_iter_info), std::get<7>(Profiler::curr_iter_info));
			spdlog::get("Logger")->info("Last SR-Score: {}; Last AR-Score: {}; Last R-Score: {}; Last G-Score: {}; Last O-Score: {}; Last D-Score: {}",
										std::get<2>(Profiler::last_sample_info), std::get<3>(Profiler::last_sample_info),
										std::get<4>(Profiler::last_sample_info), std::get<5>(Profiler::last_sample_info),
										std::get<6>(Profiler::last_sample_info), std::get<7>(Profiler::last_sample_info));
			bool accepted = false;
			double ratio = (std::get<7>(Profiler::curr_iter_info) / std::get<7>(Profiler::last_sample_info)) ;

			if (ratio >= 1.0){accepted = true;} //curr_iter_info's o-score > last_sample_info's o-score
			else
			{
				float r = ((float)rand()) / RAND_MAX;
				spdlog::get("Logger")->info("Random Prob Generated: {}", std::to_string(r));
				spdlog::get("Logger")->info("DScore / LastDScore: {}", std::to_string(ratio));
				if (r < ratio){accepted = true;}
			}

			if (accepted)
			{
				spdlog::get("Logger")->info("Accepted? Yes");
				spdlog::get("SLogger")->info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", // iter, sample, SR, AR, R, G, O, D, Acc=1
											 curr_iter, curr_sample,
											 std::get<2>(Profiler::last_sample_info), std::get<3>(Profiler::last_sample_info),
											 std::get<4>(Profiler::last_sample_info), std::get<5>(Profiler::last_sample_info),
											 std::get<6>(Profiler::last_sample_info), std::get<7>(Profiler::last_sample_info), 1);

				std::string SampleId = "sample" + std::to_string(curr_sample);
				FileManager::GetInstance()->saveSample(SampleId); // Save the sample program

				Profiler::last_sample_info = Profiler::curr_iter_info;

				// Save the best
				if (std::get<7>(Profiler::curr_iter_info) >= std::get<7>(Profiler::best_sample_info))
				{
					Profiler::best_sample_info = Profiler::curr_iter_info;
				}

				// Increase counter
				curr_sample += 1;
			}
			else // reject by Probablistic Accept Model
			{
				spdlog::get("Logger")->info("Accepted? Reject");
				spdlog::get("Logger")->info("====Revert====");
				for (auto reducefile : OptionManager::InputFiles)
				{
					reduceOneFile(reducefile);
				}
				spdlog::get("SLogger")->info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}", // iter, sample, SR, AR, R, G, O, D, Acc=0
											 curr_iter, curr_sample,
											 std::get<2>(Profiler::last_sample_info), std::get<3>(Profiler::last_sample_info),
											 std::get<4>(Profiler::last_sample_info), std::get<5>(Profiler::last_sample_info),
											 std::get<6>(Profiler::last_sample_info), std::get<7>(Profiler::last_sample_info), 0);
			}
		}
	}
	return;
}

int main(int argc, char *argv[])
{
	srand(time(0)); // Set random seed as time

	OptionManager::handleOptions(argc, argv);
	initialize();

	Profiler::GetInstance()->beginChisel();

	StatsManager::ComputeStats(OptionManager::InputFiles);
	int wc0 = std::numeric_limits<int>::max();
	int wc = StatsManager::GetNumOfWords();

	if (OptionManager::Stat)
	{
		StatsManager::Print();
		return 0;
	}

	OptionManager::PrintOptions();

	reduceProject();

	Profiler::GetInstance()->endChisel();

	Report::print();
	finalize();

	return 0;
	exit(0);
}
