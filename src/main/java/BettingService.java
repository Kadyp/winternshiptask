import java.io.*;
import java.util.*;

public class BettingService {

    private final Map<UUID, Player> players = new HashMap<>();
    private final List<UUID> illegitimatePlayers = new ArrayList<>();
    private int casinoBalance = 0;

    public void processPlayerData(String filePath) {
        File file = new File(filePath);

        if (!file.exists() || !file.canRead()) {
            System.err.println("Error: Invalid file path - " + filePath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processPlayerDataLine(line);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.err.println("Error reading or processing the file: " + e.getMessage());
        }
    }

    private void processPlayerDataLine(String line) {
        String[] tokens = line.split(",");
        if (tokens.length < 3) {
            System.err.println("Invalid line format: " + line);
            return;
        }

        UUID playerId = UUID.fromString(tokens[0]);
        Player player = players.computeIfAbsent(playerId, Player::new);

        String operation = tokens[1];
        switch (operation) {
            case "DEPOSIT" -> handleDeposit(player, tokens);
            case "BET" -> handleBet(player, tokens);
            case "WITHDRAW" -> handleWithdraw(player, tokens);
            default -> System.err.println("Unsupported operation: " + operation);
        }
    }

    private void handleDeposit(Player player, String[] tokens) {
        if (tokens.length < 3) {
            System.err.println("Invalid DEPOSIT operation: " + String.join(",", tokens));
            return;
        }

        int depositAmount = Integer.parseInt(tokens[3]);
        player.deposit(depositAmount);
    }

    private void handleBet(Player player, String[] tokens) {
        if (tokens.length < 5) {
            System.err.println("Invalid BET operation: " + String.join(",", tokens));
            return;
        }

        UUID matchId = UUID.fromString(tokens[2]);
        int betAmount = Integer.parseInt(tokens[3]);
        String betSide = tokens[4];

        if (betAmount > player.getBalance()) {
            illegitimatePlayers.add(player.getId());
            System.err.println("Illegitimate BET operation: " + String.join(",", tokens));
            return;
        }

        boolean validBet = player.withdraw(betAmount);
        if (validBet) {
            processBet(player, matchId, betAmount, betSide);
        } else {
            illegitimatePlayers.add(player.getId());
        }
    }

    private void handleWithdraw(Player player, String[] tokens) {

        int withdrawAmount = Integer.parseInt(tokens[3]);

        if (withdrawAmount > player.getBalance()) {
            illegitimatePlayers.add(player.getId());
            System.err.println("Illegitimate WITHDRAW operation: " + String.join(",", tokens));
        } else {
            player.withdraw(withdrawAmount);
        }
    }

    public void processMatchData(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                UUID matchId = UUID.fromString(tokens[0]);
                double rateA = Double.parseDouble(tokens[1]);
                double rateB = Double.parseDouble(tokens[2]);
                char result = tokens[3].charAt(0);

                Match match = new Match(matchId, rateA, rateB, result);
                calculateCasinoBalance(match);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void calculateResults() {
        try (PrintWriter writer = new PrintWriter("result.txt")) {
            processPlayerData("data/player_data.txt");
            writeLegitimatePlayersResults(writer);
            writer.println();
            writeIllegitimatePlayersResults(writer);
            writer.println();
            writeCasinoBalance(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLegitimatePlayersResults(PrintWriter writer) {
        players.values().stream()
                .filter(player -> !illegitimatePlayers.contains(player.getId()))
                .sorted(Comparator.comparing(Player::getId))
                .forEach(player -> writer.printf("%s %d %.2f%n",
                        player.getId(), player.getBalance(), player.getWinRate()));
    }

    private void writeIllegitimatePlayersResults(PrintWriter writer) {
        illegitimatePlayers.stream()
                .sorted(UUID::compareTo)
                .forEach(playerId -> {
                    Player player = players.get(playerId);
                    List<String> operationTokens = new ArrayList<>();
                    operationTokens.add(player.getId().toString());
                    operationTokens.addAll(getFirstIllegalOperation(player));
                    writer.println(String.join(" ", operationTokens));
                });
    }

    private List<String> getFirstIllegalOperation(Player player) {
        for (String line : getTransactionLines("data/player_data.txt")) {
            String[] tokens = line.split(",");
            UUID playerId = UUID.fromString(tokens[0]);
            if (playerId.equals(player.getId())) {
                return Arrays.asList(tokens[0], tokens[1], tokens[2], tokens[3]);
            }
        }
        return Collections.emptyList();
    }

    private void writeCasinoBalance(PrintWriter writer) {
        writer.println(casinoBalance);
    }

    private void processBet(Player player, UUID matchId, int betAmount, String betSide) {
        Match match = getMatchById(matchId);
        if (match != null) {
            char result = match.getResult();
            player.placeBet(betAmount, result == betSide.charAt(0));
        }
    }

    private void calculateCasinoBalance(Match match) {
        List<String> transactionLines = getTransactionLines("data/player_data.txt");
        for (String line : transactionLines) {
            String[] tokens = line.split(",");

            if ("BET".equals(tokens[1]) && tokens.length >= 5) {
                UUID playerId = UUID.fromString(tokens[0]);
                int betAmount = Integer.parseInt(tokens[3]);
                String betSide = tokens[4];

                if (illegitimatePlayers.contains(playerId)) {
                    continue;
                }

                Player player = players.get(playerId);
                if (player != null && match.getId().equals(UUID.fromString(tokens[2]))) {
                    char result = match.getResult();
                    boolean betOutcome = result == betSide.charAt(0);

                    if (betOutcome) {
                        casinoBalance += betAmount * getWinningCoefficient(match, betSide);
                    } else {
                        casinoBalance -= betAmount;
                    }
                }
            }
        }
    }
    private double getWinningCoefficient(Match match, String betSide) {
        if (betSide.equals("A")) {
            return match.getRateA();
        } else if (betSide.equals("B")) {
            return match.getRateB();
        } else {
            return 0.0;
        }
    }

    private Match getMatchById(UUID matchId) {
        try (BufferedReader reader = new BufferedReader(new FileReader("data/match_data.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                UUID id = UUID.fromString(tokens[0]);
                if (id.equals(matchId)) {
                    double rateA = Double.parseDouble(tokens[1]);
                    double rateB = Double.parseDouble(tokens[2]);
                    char result = tokens[3].charAt(0);
                    return new Match(id, rateA, rateB, result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> getTransactionLines(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}


