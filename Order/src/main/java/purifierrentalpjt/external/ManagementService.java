
package purifierrentalpjt.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//@FeignClient(name="Management", url="http://localhost:8084")
@FeignClient(name="Management", url="http://management:8080")
public interface ManagementService {

    @RequestMapping(method= RequestMethod.POST, path="/managements")
    public void completeSurvey(@RequestBody Management management);

}