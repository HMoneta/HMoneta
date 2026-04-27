package fan.summer.hmoneta.database.repository.task;

import fan.summer.hmoneta.database.entity.task.TaskConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskConfigRepository extends JpaRepository<TaskConfigEntity, String> {
}
