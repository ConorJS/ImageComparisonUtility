package threading;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class EventTimer {

    private long timeBefore, timeAfter, nanoBefore, nanoAfter;

    public EventTimer() {
        timeBefore = LocalDateTime.now().toEpochSecond((ZoneOffset.ofHours(8)));
        nanoBefore = LocalDateTime.now().getNano();
    }

    public int endTimer() {
        timeAfter = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        nanoAfter = LocalDateTime.now().getNano();

        return (int)(((timeAfter - timeBefore) * 1000) + ((nanoAfter - nanoBefore) / 1000000));
    }

}
