import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.UnavailableTimeBlock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Arrays;

public class DebugTest {
    public static void main(String[] args) {
        Teacher teacher = new Teacher();
        teacher.setName("Test Teacher");
        
        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );
        
        System.out.println("Before set:");
        System.out.println("unavailableTimes field: " + teacher.getUnavailableTimes());
        System.out.println("getUnavailableTimeBlocks(): " + teacher.getUnavailableTimeBlocks());
        
        teacher.setUnavailableTimeBlocks(Arrays.asList(block));
        
        System.out.println("\nAfter set:");
        System.out.println("unavailableTimes field: " + teacher.getUnavailableTimes());
        System.out.println("getUnavailableTimeBlocks(): " + teacher.getUnavailableTimeBlocks());
        System.out.println("Size: " + teacher.getUnavailableTimeBlocks().size());
    }
}
