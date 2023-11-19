public class Main {
    public static void main(String[] args) {
            BettingService bettingService = new BettingService();
            bettingService.processPlayerData("data/player_data.txt");
            bettingService.processMatchData("data/match_data.txt");
            bettingService.calculateResults();
        }
    }