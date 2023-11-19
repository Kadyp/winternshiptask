import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class Player {
    private UUID id;
    private long balance;
    private int totalBets;
    private int totalWins;

    public Player(UUID id) {
        this.id = id;
        this.balance = 0;
        this.totalBets = 0;
        this.totalWins = 0;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public boolean withdraw(int amount) {
        if (amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public void placeBet(int amount, boolean won) {
        totalBets++;
        if (won) {
            totalWins++;
            balance += amount;
        } else {
            balance -= amount;
        }
    }

    public double getWinRate() {
        return totalBets == 0 ? 0 : (double) totalWins / totalBets;
    }
}
