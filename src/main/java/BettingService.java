import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

    public class BettingService {
        public static void processBettingData() {
            try {
                // Parse input data
                Path playerDataPath = Paths.get("src/data/player_data.txt");
                Path matchDataPath = Paths.get("src/data/match_data.txt");

                Map<UUID, Player> players = DataParser.parsePlayerData(playerDataPath);
                List<Match> matches = DataParser.parseMatchData(matchDataPath);

                // Process player actions
                ActionProcessor.processPlayerActions(players, bets, deposits, withdrawals);

                // Calculate casino host balance
                long casinoBalance = CasinoBalanceCalculator.calculateCasinoBalance(bets, matches);

                // Generate results
                List<String> results = ResultsGenerator.generateResults(players, bets, casinoBalance);

                // Write results to file
                Path outputPath = Paths.get("src/main/resources/results.txt");
                ResultsWriter.writeResults(outputPath, results);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

