import com.icsd.springor.DTO.AssignmentDTO;
import com.icsd.springor.model.Assignment;
import com.icsd.springor.model.Course;
import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.model.User;
import com.icsd.springor.repository.AssignmentRepository;
import com.icsd.springor.repository.CourseRepository;
import com.icsd.springor.repository.CourseScheduleRepository;
import com.icsd.springor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseScheduleRepository scheduleRepository;
    
    public AssignmentDTO createAssignment(Long courseId, Long teacherId, 
                                        Course.TeachingHours.CourseComponent component, 
                                        Long scheduleId) {
        
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));
        
        CourseSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + scheduleId));
        
        // Check if assignment already exists
        if (assignmentRepository.existsByCourseAndCourseComponentAndSchedule(course, component, schedule)) {
            throw new RuntimeException("Assignment already exists for this course component");
        }
        
        // Check if schedule is in correct phase
        if (schedule.getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot create assignments. Schedule is not in assignment phase.");
        }
        
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTeacher(teacher);
        assignment.setCourseComponent(component);
        assignment.setSchedule(schedule);
        assignment.setActive(true);
        
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToDTO(savedAssignment);
    }
    
    public List<AssignmentDTO> getAssignmentsBySchedule(Long scheduleId) {
        List<Assignment> assignments = assignmentRepository.findActiveAssignmentsBySchedule(scheduleId);
        return assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<AssignmentDTO> getAssignmentsByTeacher(Long teacherId) {
        List<Assignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        return assignments.stream()
                .filter(Assignment::isActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<AssignmentDTO> getAssignmentsByTeacherAndSchedule(Long teacherId, Long scheduleId) {
        List<Assignment> assignments = assignmentRepository.findByTeacherAndSchedule(teacherId, scheduleId);
        return assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public AssignmentDTO updateAssignment(Long assignmentId, Long newTeacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
        
        // Check if schedule allows modifications
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot modify assignments. Schedule is not in assignment phase.");
        }
        
        User newTeacher = userRepository.findById(newTeacherId)
            .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + newTeacherId));
        
        assignment.setTeacher(newTeacher);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return convertToDTO(savedAssignment);
    }
    
    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment not found with id: " + assignmentId));
        
        // Check if schedule allows modifications
        if (assignment.getSchedule().getStatus() != CourseSchedule.ScheduleStatus.ASSIGNMENT_PHASE) {
            throw new RuntimeException("Cannot delete assignments. Schedule is not in assignment phase.");
        }
        
        assignmentRepository.delete(assignment);
    }
    
    public boolean areAllCoursesAssigned(Long scheduleId) {
        // Get all courses that should be assigned
        List<Course> allCourses = courseRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findActiveAssignmentsBySchedule(scheduleId);
        
        // Check if every course component has an assignment
        for (Course course : allCourses) {
            if (!course.isActive()) continue;
            
            // Check if course has theory component and if it's assigned
            boolean hasTheory = course.getTeachingHours().stream()
                .anyMatch(th -> th.getComponent() == Course.TeachingHours.CourseComponent.THEORY);
            if (hasTheory) {
                boolean theoryAssigned = assignments.stream()
                    .anyMatch(a -> a.getCourse().getId().equals(course.getId()) && 
                              a.getCourseComponent() == Course.TeachingHours.CourseComponent.THEORY);
                if (!theoryAssigned) return false;
            }
            
            // Check if course has lab component and if it's assigned
            boolean hasLab = course.getTeachingHours().stream()
                .anyMatch(th -> th.getComponent() == Course.TeachingHours.CourseComponent.LABORATORY);
            if (hasLab) {
                boolean labAssigned = assignments.stream()
                    .anyMatch(a -> a.getCourse().getId().equals(course.getId()) && 
                              a.getCourseComponent() == Course.TeachingHours.CourseComponent.LABORATORY);
                if (!labAssigned) return false;
            }
        }
        
        return true;
    }
    
    private AssignmentDTO convertToDTO(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        dto.setCourseId(assignment.getCourse().getId());
        dto.setCourseName(assignment.getCourse().getName());
        dto.setCourseCode(assignment.getCourse().getCode());
        dto.setTeacherId(assignment.getTeacher().getId());
        dto.setTeacherName(assignment.getTeacher().getFullName());
        dto.setTeacherUsername(assignment.getTeacher().getUsername());
        dto.setCourseComponent(assignment.getCourseComponent());
        dto.setActive(assignment.isActive());
        if (assignment.getSchedule() != null) {
            dto.setScheduleId(assignment.getSchedule().getId());
        }
        return dto;
    }
}