import com.heronix.model.domain.Teacher;
import com.heronix.model.dto.UnavailableTimeBlock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class SimpleTeacherTest {
    public static void main(String[] args) {
        System.out.println("=== Simple Teacher Availability Test ===\n");

        Teacher teacher = new Teacher();
        teacher.setName("John Smith");

        System.out.println("1. Initial state:");
        System.out.println("   unavailableTimes (String): " + teacher.getUnavailableTimes());
        System.out.println("   getUnavailableTimeBlocks(): " + teacher.getUnavailableTimeBlocks());
        System.out.println();

        UnavailableTimeBlock block = new UnavailableTimeBlock(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            "Department Meeting",
            true
        );

        System.out.println("2. Calling setUnavailableTimeBlocks with 1 block...");
        teacher.setUnavailableTimeBlocks(List.of(block));

        System.out.println("3. After set:");
        System.out.println("   unavailableTimes (String): " + teacher.getUnavailableTimes());
        System.out.println("   getUnavailableTimeBlocks(): " + teacher.getUnavailableTimeBlocks());
        System.out.println("   Size: " + teacher.getUnavailableTimeBlocks().size());

        System.out.println("\n=== Test Complete ===");
    }
}
