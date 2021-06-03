package purifierrentalpjt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Management Command
 * @author Administrator
 *
 */
 @RestController
 public class ManagementController {

    @Autowired
    ManagementRepository managementRepository;

    /**
     * 설문 완료
     * @param management
     */
    @RequestMapping(method=RequestMethod.POST, path="/management")
    public void surveyCompletion(@RequestBody Management management) {
    	
    	System.out.println( "### 동기호출 -설문완료");

    	Optional<Management> opt = managementRepository.findByOrderId(management.getOrderId());
    	if( opt.isPresent()) {
    		Management surveyComplete =opt.get();
    		surveyComplete.setStatus("surveyCompleted");
    		managementRepository.save(surveyComplete);
    	} else {
    		System.out.println("### 설문 - 못찾음");
    	}
    }

 }
