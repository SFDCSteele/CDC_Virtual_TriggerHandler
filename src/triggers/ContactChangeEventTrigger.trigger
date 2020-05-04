trigger ContactChangeEventTrigger on ContactChangeEvent (after insert) {
    
    System.debug('ContactChangeEventTrigger...running...');
    Boolean DisableTriggersFlagPermission = FeatureManagement.checkPermission('DisableTriggersFlag');    
    ContactChangeEventTriggerHandler cceth = new ContactChangeEventTriggerHandler();
    if ( !DisableTriggersFlagPermission ) {
	    new ContactChangeEventTriggerHandler().run();
    }
    
}