/// <reference path="../../../plugins/cordova-plugin-mfp-push/typings/mfppush.d.ts" />
import { Injectable } from '@angular/core';
declare var MFPPush: any;


/*
  Generated class for the PushserviceProvider provider.

  See https://angular.io/guide/dependency-injection for more info on providers
  and Angular DI.
*/
@Injectable()
export class PushserviceProvider {
  tagname: any;
  dataRequest: any;

  constructor() {
    console.log('PushserviceProvider constructor called');   
     
  }
  load(tagname){
    console.log('Push load invoked:' + tagname);
    // encode spaces in tagname
    let temp=tagname;
    
    let tag = temp.replace(/\s/g,"%20");
    console.log('tag:' + tag);
    let subscribeFail = false;

    //adapter call to create tag
    this.dataRequest = new WLResourceRequest("/adapters/PushAPI/createTag/"+tag, WLResourceRequest.POST);
    this.dataRequest.send().then(
      (response) => {
        console.log('--> Push service tag created:' + tagname);   
        MFPPush.initialize (
          function(successResponse) {
              console.log('--> push init success');
              MFPPush.registerNotificationsCallback(notificationReceived);
              MFPPush.registerDevice(null,
                function(successResponse) {
                   console.log('Successfully registered');
                   var pushtag=[tagname];
                   MFPPush.subscribe(
                    pushtag,
                    function(success) {                        
                        alert('Subscribed successfully');
                    },
                    function(faliure) {
                        console.log("--> push subscribe fail");
                        subscribeFail = true;
                    })
                },
                function(failure) {
                    console.log('Failed to register' + JSON.stringify(failure));
                    subscribeFail = true;
                }
              )
            },
          function(failureResponse) {
              console.log('Failed to initialize');
              subscribeFail = true;
          }
        )     
      }, (failure) => {
        console.log('--> PushProvider tag creation failed\n', JSON.stringify(failure)); 
        subscribeFail = true;     
      })

      if (subscribeFail){
        alert('Subscription Failed');
      }
    

  function notificationReceived(message)
  {
    alert(message.alert);
  }

  }

}
