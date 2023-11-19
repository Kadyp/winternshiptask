import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Match {
    private UUID id;
    private double rateA;
    private double rateB;
    private char result;

    public Match(UUID id, double rateA, double rateB, char result) {
        this.id = id;
        this.rateA = rateA;
        this.rateB = rateB;
        this.result = result;
    }

}
