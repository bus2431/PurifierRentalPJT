package purifierrentalpjt;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface ManagementRepository extends CrudRepository<Management, Long>{
    Optional<Management> findByOrderId(Long orderId);

}
