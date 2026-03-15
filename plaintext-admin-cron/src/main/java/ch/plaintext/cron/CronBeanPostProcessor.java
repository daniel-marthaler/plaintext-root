package ch.plaintext.cron;

import ch.plaintext.PlaintextCron;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * BeanPostProcessor that wraps PlaintextCron implementations into SuperCron at runtime.
 * This allows cron jobs to only implement the simple PlaintextCron interface,
 * while automatically gaining all the functionality of SuperCron.
 *
 * @author : mad
 * @since : 30.11.2025
 */
@Slf4j
@Component
public class CronBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // Check if bean implements PlaintextCron but is NOT already a SuperCron
        if (bean instanceof PlaintextCron && !(bean instanceof SuperCron)) {

            log.info("Wrapping PlaintextCron bean '{}' of type {} into SuperCron",
                     beanName, bean.getClass().getName());

            PlaintextCron cronLogic = (PlaintextCron) bean;
            Class<? extends PlaintextCron> originalClass = (Class<? extends PlaintextCron>) bean.getClass();

            // Create a SuperCron wrapper at runtime
            SuperCron wrapper = new SuperCron() {
                @Override
                public void run(String mandant) {
                    cronLogic.run(mandant);
                }

                @Override
                public boolean isGlobal() {
                    // Delegate to the wrapped implementation
                    return cronLogic.isGlobal();
                }

                @Override
                public String getName() {
                    // Return the simple name of the original class, not the wrapper
                    return originalClass.getSimpleName();
                }

                @Override
                public String getDisplayName() {
                    // Delegate to the wrapped implementation
                    return cronLogic.getDisplayName();
                }

                @Override
                public String getDefaultCronExpression() {
                    // Delegate to the wrapped implementation
                    return cronLogic.getDefaultCronExpression();
                }
            };

            // Set Spring properties on the wrapper
            wrapper.setBeanName(beanName);
            wrapper.setApplicationContext(applicationContext);
            wrapper.setOriginalBeanClass(originalClass);

            // Note: We don't call afterPropertiesSet() here because:
            // 1. The wrapper is not registered in the context yet
            // 2. The singleton check would fail
            // The bean scope check will happen when the original bean was created

            return wrapper;
        }

        return bean;
    }
}
