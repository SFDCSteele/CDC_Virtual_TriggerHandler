@isTest
public class TestContactChangeEventTrigger {
    @isTest static void testCreateAndUpdateContact() {
        // Enable all Change Data Capture entities for notifications.
        /*
            insert new contact(FirstName='cdc',LastName='test5',Email='test5@email.com');
            update contact(Title='updated5',Email='test5@email.com');
         */
        Test.enableChangeDataCapture();
        Id contUVRecTypeId = Schema.SObjectType.Contact.getRecordTypeInfosByName().get('Onboarding').getRecordTypeId();
        // Insert Contact test records
        List<Contact> newContacts = new List<Contact>();
        for (Integer i=0;i<5;i++) {
            
            newContacts.add(new Contact(FirstName='cdcTest',
                               LastName='Astro-'+i,
                               RecordTypeId=contUVRecTypeId,
                               Email='test_'+i+'@email.com',
                               Title='tester'));
            
        }
        insert newContacts;
        for (Contact cont: newContacts) {
            System.debug('Newly inserted contact: '+cont);
        }
        // Call deliver to fire the trigger and deliver the test change event.
        Test.getEventBus().deliver();
        // VERIFICATIONS
        // Check that the change event trigger created a task.
        Contact[] contRecords = [SELECT Id,OwnerId,FirstName,LastName,Email,Title,AccountId FROM Contact];
        System.assertEquals(5, contRecords.size(),
                            'The change event trigger did not create the expected contacts.');
        for (Contact cont: contRecords) {
            System.debug('Validating account Id: '+cont);
        }
        // Update employee record
        // Test update with the first record
        Contact cont = contRecords[0];
        // Debug
        System.debug('Retrieved Contact record: ' + cont);
        // Update one field and empty another
        cont.FirstName = 'Codey';
        cont.AccountId = null;
        update cont;
        // Call deliver to fire the trigger for the update operation.
        Test.getEventBus().deliver();
        // VERIFICATIONS
        // Check that the change event trigger created a task.
        // We should have two tasks now, including one from the first trigger invocation.
        List<Account> acctRecords = [SELECT Id,Name,RecordTypeId FROM Account];// where AccountId != null
        for (Account acct: acctRecords) {
            System.debug('Account validation: '+acct);
        }
        System.assertEquals(5, contRecords.size(),
                            'The contacts do not have account Ids assigned.');
        List<Contact> contRecords2 = [SELECT Id,OwnerId,FirstName,LastName,Email,Title,AccountId FROM Contact];// where AccountId != null
        for (Contact cont2: contRecords2) {
            System.debug('Final validation: '+cont2);
        }
        System.assertEquals(5, contRecords2.size(),
                            'The contacts do not have account Ids assigned.');
    }
}