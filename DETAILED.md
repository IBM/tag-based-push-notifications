## Steps
1. [Use Ionic-MFP-App as a starting point for this project](#step-1-use-ionic-mfp-app-as-a-starting-point-for-this-project)
2. [Support Facebook and Google Login](#step-3-support-facebook-and-google-login)
  - 2.1 [Enabling the server](#21-enabling-the-server)
  - 2.2 [Enabling the client](#22-enabling-the-client)
3. Support tag based Push notification
 - 3.1 [Enabling the server](#31-enabling-the-server)
  - 3.2 [Enabling the client](#32-enabling-the-client)


## Step 1. Use Ionic-MFP-App as a starting point for this project

This project builds on top of the app built in https://github.com/IBM/Ionic-MFP-App (referred to as base pattern in sections below). In this code pattern, we will enhance the 
app with social Login authentication mechanism and tag based Push notifications.



Copy Ionic Mobile app and Mobile Foundation adapters from parent repo as per instructions in 
http://bit-traveler.blogspot.in/2012/08/git-copy-file-or-directory-from-one.html as shown below.

* Create your repo on [Github.com](https://github.com) and add `README.md` file. Clone your new repo.

```
$ git clone https://github.com/<your-username>/<your-new-repo-name>.git
```

* Make a git format-patch for the entire history of the sub-directories that we want as shown below.

```
$ mkdir gitpatches
$ git clone https://github.com/IBM/Ionic-MFP-App.git
$ cd Ionic-MFP-App
$ git format-patch -o ../gitpatches/ --root IonicMobileApp/ MobileFoundationAdapters/
```

We will be using only the UserLogin and SocialLogin adapters from this base project.
There are modifications to the MyWardData adapter and PushAPI adapter is newly added for this pattern. So please use these 2 adapters from the Push notification code pattern only.


* Import the patches into your new repository as shown below.

```
$ cd ../<your-new-repo-name>
$ git am ../gitpatches/*
$ git push
```



## Step 2. Support Facebook and Google Login

### 2.1 Enabling the server

In this pattern, the 'SocialLogin' security check is responsible for validating the challenge that was sent from the client with the social platform token.
The security check expects to get the JSON response from the app as described:
```
{
  "vendor" : "...",
  "token"  : "..."
}
```
The SocialLoginAdapter available under MobileFoundationAdapters implements this securitycheck.
For more information on the securitycheck implementation please refer to this link - https://mobilefirstplatform.ibmcloud.com/blog/2016/04/06/social-login-with-ibm-mobilefirst-platform-foundation/
Deploy this adapter following the instructions in this repository Readme.

### 2.2 Enabling the client

#### 2.2.1 Add the login buttons in the application login screen

Add the following code in the login.html file post the 'Sign In' Button.
<pre><code>
&lt;div padding&gt;
      &lt;button ion-button block (click)="fbLogin()" icon-start&gt;
        &lt;ion-icon name="logo-facebook"&gt;&1l;/ion-icon&gt;
        Facebook Login
        &lt;/button&gt;
&lt;/div&gt;


&lt;div padding>
      &lt;button ion-button block (click)="googleLogin()" icon-start&gt;
          &lt;ion-icon name="logo-googleplus">&lt;/ion-icon&gt;
        Google Login
      &lt;/button&gt;
    &lt;/div&gt;

</code></pre>

#### 2.2.2 Add the onclick methods

Add the following methods in the login.ts file.


<pre><code>
fbLogin() {

    this.authHandler.facebooklogin();   
  }

googleLogin(){

    this.authHandler.googlePlusLogin();    
  }
</pre></code>

####2.2.3 Add the login methods in the authHandler Provider

Add the following code in the authHandler.ts file post the login method
facebookLogin() method invokes the facebook.login method and receives a token. This token is passed to loginWithFb() method where the  MFP WLAuthorization login API is invoked to validate the credentials using the SocialLogin securitycheck. Similar logic is applied by the google login methods - googlePlusLogin() and  loginWithGoogle().

<pre><code>

facebooklogin(){
    this.fb.login(['public_profile', 'user_friends', 'email'])
    .then(res => { 
      if(res.status === "connected") {
        this.fb.api('me?fields=id,name,email,first_name', []).then(profile => {
          this.userData = {email: profile['email'], first_name: profile['first_name']}
          this.username = this.userData.email;        
        });      
        var accessToken = res.authResponse.accessToken;
        console.log(accessToken);
        this.loader = this.loadingCtrl.create({
          content: 'Signining in. Please wait ...',
          dismissOnPageChange: true
        });
        this.loader.present().then(() => {
          this.loginWithFb(accessToken);
        });
      
    }
  })
  .catch(e => console.log('Error logging into Facebook', e)); 
  }

 loginWithFb(accessToken){
    console.log('--> AuthHandler loginwithfb called ');
    var credentials = { 'token': accessToken, 'vendor': 'facebook' };
    if (this.isChallenged) {
      this.socialLoginChallengeHandler.submitChallengeAnswer(credentials);
    } else {
      // https://stackoverflow.com/questions/20279484/how-to-access-the-correct-this-inside-a-callback
      var self = this;
      WLAuthorizationManager.login(this.securityCheckNameSocial, credentials)
      .then(
        (success) => {
          console.log('--> AuthHandler: login success');
          this.loginSecurityCheck = 'socialLogin';
          this.fbLoginStatus = 'connected';
          
        },
        (failure) => {
          console.log('--> AuthHandler: login failure: ' + JSON.stringify(failure));
          self.loginFailureCallback(failure.errorMsg);
        }
      );
    }
  }

  googlePlusLogin(){
 
    this.googlePlus.login({
      'scopes': '',
      'webClientId': '618106571370-pr9058fhv2efj4635ertkgbn14tda2ha.apps.googleusercontent.com',
      'offline': true
    })
	  .then(res => {        
          console.log(res);
          this.username = res.email;
          var accessToken = res.idToken;
          console.log(accessToken);
          this.loader = this.loadingCtrl.create({
            content: 'Signining in. Please wait ...',
            dismissOnPageChange: true
          });
          this.loader.present().then(() => {
            this.loginWithGoogle(accessToken);
          });
         
     
    })
    .catch(e => console.log('Error logging into Google', e)); 
  }
  
  loginWithGoogle(accessToken){
	   console.log('--> AuthHandler loginwithGoogle called ');
    var credentials = { 'token': accessToken, 'vendor': 'google' };
    if (this.isChallenged) {
      this.socialLoginChallengeHandler.submitChallengeAnswer(credentials);
    } else {
      // https://stackoverflow.com/questions/20279484/how-to-access-the-correct-this-inside-a-callback
      var self = this;
      WLAuthorizationManager.login(this.securityCheckNameSocial, credentials)
      .then(
        (success) => {
          console.log('--> AuthHandler: login success');
          this.loginSecurityCheck = 'socialLogin';
          this.googleLoginStatus = 'connected';
          
        },
        (failure) => {
          console.log('--> AuthHandler: login failure: ' + JSON.stringify(failure));
          self.loginFailureCallback(failure.errorMsg);
        }
      );
    }  
	}

</code></pre>

#### 2.2.4 Update the Resource Adapter invocation calls

The WLResourceRequest calls in my-ward-data.ts invoke the procedures in MyWardData adapter to get the application data. The application has 2 types of logins - enterprise login or the social login mapped to 2 security checks - UserLogin and SocialLogin. The logged in user would have either cleared the UserLogin security check or the SocialLogin security check but not both, access has to be given to that user.
MyWardData adapter has the respective endpoints which are protected by each of these security checks.

In my-ward-data.ts, replace the WLResourceRequest calls with the following code.
The latitude, longitude parameters are used in the Cloudant geospatial query executed by the MyWardData adapter to retrieve all the grievances within a particular radius of the user current location.

load() :

<pre><code>

if (this.authHandler.getLoginSecurityCheck()== 'UserLogin')
	  {      
		  this.dataRequest = new WLResourceRequest("/adapters/MyWardData/userLogin", WLResourceRequest.GET);
	  }
	  else {
		  this.dataRequest = new WLResourceRequest("/adapters/MyWardData/socialLogin", WLResourceRequest.GET);
	  }

      this.dataRequest.setQueryParameter("lat", this.lat);
      this.dataRequest.setQueryParameter("lon",this.lng);
      this.dataRequest.setQueryParameter("radius", this.range);
 

</code></pre> 


getObjectStorageAccess() :
<pre><code>
if (this.authHandler.getLoginSecurityCheck() == 'UserLogin')
	  {
		   this.dataRequest = new WLResourceRequest("/adapters/MyWardData/userLogin/objectStorage", WLResourceRequest.GET);
	  }
	  else {
		   this.dataRequest = new WLResourceRequest("/adapters/MyWardData/socialLogin/objectStorage", WLResourceRequest.GET);
	  }



</code></pre>

uploadNewGrievance() :
<pre><code>
if (this.authHandler.getLoginSecurityCheck() == 'UserLogin')
	  {
		  this.dataRequest = new WLResourceRequest("/adapters/MyWardData/userLogin", WLResourceRequest.POST);
	  }
	  else {
		  this.dataRequest = new WLResourceRequest("/adapters/MyWardData/socialLogin", WLResourceRequest.POST);
	  }

</code></pre>

#### 3.2.5 Add the logout functionality

Add the logout icon post the 'add' icon in the home.html file as shown below


<pre><code>

&lt;button ion-button icon-only (click)="reportNewProblem()"&gt;
        &lt;ion-icon name="add"&gt;&lt;/ion-icon&gt;
&lt;/button>
&lt;button ion-button icon-only (click)="logout()"&gt;
        &lt;ion-icon name="log-out"&gt;&lt;/ion-icon&gt;
&lt;/button&gt;

</code></pre>

Add the following logout code in the home.ts file

<pre><code>

logout(){

   
    this.authHandler.logout();
    this.navCtrl.setRoot(LoginPage);
    
    
  }     

</code></pre>

Implement the logout code in authHandler.ts as below

<pre><code>

logout() {

    console.log('--> AuthHandler logout called');
    WLAuthorizationManager.logout(this.securityCheckName)
    .then(
      (success) => {
        console.log('--> AuthHandler: logout success');
      },
      (failure) => {
        console.log('--> AuthHandler: logout failure: ' + JSON.stringify(failure));
      }
    );
    if(this.googleLoginStatus === 'connected')
    {  
      console.log('--> AuthHandler: logging out from Google');
      this.googlePlus.logout();
    }
    if(this.fbLoginStatus === 'connected')
    {
      console.log('--> AuthHandler: logging out from Facebook');
      this.fb.logout();
    }
    

</code></pre>

##3.  Support tag based Push notification

### 3.1 Enabling the server
Let us see the changes that are required to be made on the server side code of the base pattern(https://github.com/IBM/Ionic-MFP-App )

#### 3.1.1 Push API adapter

PushAPI adapter is a resource adapter used by this Ionic application to create a tag on the MobileFoundation server if it does not exist already and also used to send push notifications. Please refer to the adapter source code available in this repository. Build and deploy the adapter to the Mobile Foundation server.

#### 3.1.2 MyWardData adapter
In the base pattern, MyWardData adapter fetches the grievances that are loaded in the Cloudant database. In the current pattern, adapter code is modified to fetch the grievances within a a given radius of the user current location or any location selected by the user. Cloudant geospatial index query is used to achieve this. 'geodd' is the design document and geoidx is the geospatial index in cloudant database.


<pre><code>
public Response getAllEntries_ul(
			@QueryParam(value = "lat") double latitude,
			@QueryParam(value = "lon") double longitude,
			@QueryParam(value = "radius") double radius) 
			throws Exception {
		
		try {
			
			 StringBuilder sb = new StringBuilder();
			 sb.append('/')
			   .append(configurationAPI.getPropertyValue("DBName"))
			   .append("/_design/geodd/_geo/geoidx");

			.................

</code></pre>

 
Please refer to the complete source code of the MyWardData adapter in this repository. 
Build and deploy the adapter to the Mobile Foundation server.
  
### 3.2 Enabling the client
Let us see the changes that are required to be made on the client side code of the base pattern(https://github.com/IBM/Ionic-MFP-App )

#### 3.2.1 Add Watch Area page
Add a new page in the ionic app under  IonicMobileApp/src/pages/ with name 'watch-area'.
This page will be invoked on click of the 'Watch Area' icon on the home page. The user will be able to select a location in the map and submit to see the grievances within a given radius of that particular location. Also the user can subscribe to receive any notifications on any new grievances submitted on that particular location. Google GeoCoding API and cloudant geospatial index queries are used to handle the location coordinates related actions.

Update the  IonicMobileApp/src/pages/watch-area/watch-area.html as below

<pre><code>

&lt;ion-header&gt;
  &lt;ion-navbar&gt;
    &lt;ion-title&gt;watch-area&lt;/ion-title&gt;
  &lt;/ion-navbar&gt;
&lt;/ion-header&gt;
&lt;ion-content padding&gt;
  &lt;div id="map"&gt;&lt;/div&gt;
  &lt;b>Select location on map&lt;/b&gt;  
  &lt;div&gt;
    &lt;ion-grid&gt;
        &lt;ion-row&gt;
          &lt;ion-col col-6&gt;
              &lt;button ion-button full (click)="submit()"&gt;
                  &lt;ion-icon name="cloud-upload"&gt;&lt;/ion-icon&gt;
                    Submit
                &lt;/button&gt;  
          &lt;/ion-col&gt;
          &lt;ion-col col-6&gt;
              &lt;button ion-button full (click)="subscribe()"&gt;
                  &lt;ion-icon name="subscribe"&gt;&lt;/ion-icon&gt;
                    Receive Notifications
                &lt;/button&gt; 
          &lt;/ion-col&gt;
        &lt;/ion-row&gt;
      &lt;/ion-grid&gt;

</code></pre>

Update the  IonicMobileApp/src/pages/watch-area/watch-area.scss as below

<pre><code>
page-watch-area {
    #map {
        height: 80%;
        width: 80%;
    }
             
}
</code>
</pre>

Update the  IonicMobileApp/src/pages/watch-area/watch-area.ts as below


<pre><code>
import { Component } from '@angular/core';
import { IonicPage, NavController, AlertController, NavParams, ViewController } from 'ionic-angular';
import { GoogleMaps, GoogleMap, GoogleMapsEvent, GoogleMapOptions, Marker, LatLng } from '@ionic-native/google-maps';
import { NativeGeocoder, NativeGeocoderReverseResult } from '@ionic-native/native-geocoder';
import { HomePage } from '../home/home'
import { PushserviceProvider } from '../../providers/pushservice/pushservice';

/**
 * Generated class for the WatchAreaPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@IonicPage()
@Component({
  selector: 'page-watch-area',
  templateUrl: 'watch-area.html',
})
export class WatchAreaPage {

  map: GoogleMap;
  mapReady: boolean = false;
  location: LatLng = null;
  range: any='';
  subLocality: any;
  locality: any;
  subAdminArea: any;
  adminArea: any;

  constructor(public navCtrl: NavController, public navParams: NavParams, private alertCtrl: AlertController, 
    public viewCtrl: ViewController,public push: PushserviceProvider,public Geocoder: NativeGeocoder ) {
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad WatchAreaPage');
    this.createMap();
  }

  createMap() {
    // TODO need to store/retrieve prevLoc in app preferences/local storage
    console.log('Watch Area called');
    let prevLoc = new LatLng(13.0768342, 77.7886087);
    let mapOptions: GoogleMapOptions = {
      camera: {
        target: prevLoc,
        zoom: 15,
        tilt: 10
      }
    };
    this.map = GoogleMaps.create('map', mapOptions);
    console.log('--> WatchArea: map created');
    this.map.one(GoogleMapsEvent.MAP_READY).then(() => {
      console.log('--> WatchArea: Map is Ready To Use');
      this.mapReady = true;
      // https://stackoverflow.com/questions/4537164/google-maps-v3-set-single-marker-point-on-map-click
      this.map.on(GoogleMapsEvent.MAP_CLICK).subscribe( event => {
        this.location = event[0];
        console.log('--> WatchAreaPage: User clicked location1 = ' + event[0]);          
        this.map.clear();      
        this.map.addMarker({
          title: 'Selected location',
          position: event[0]
        }).then((marker: Marker) => {
          marker.showInfoWindow();
        });
     
      });
    });
  }


  captureLocation() {    
      // Move the map camera to the location with animation
      this.map.animateCamera({
        target: this.location,
        zoom: 14,
        tilt: 30
      }).then(() => {
        //add a circle
        console.log('Adding a circle'+ JSON.stringify(this.location));
        console.log('range'+ this.range);
        this.map.addCircle({
          'center': this.location,
          'radius': this.range,
          'strokeColor' : '#AA00FF',
          'strokeWidth': 5,
          'fillColor' : '#880000'
        });       
      })    
  }

  submit()
  {

    this.navCtrl.push(HomePage, { lat: this.location.lat, lng: this.location.lng} );
      
  }
  subscribe()
  {
    
    this.Geocoder.reverseGeocode(this.location.lat, this.location.lng)
    .then((result: NativeGeocoderReverseResult[]) => {
    console.log(JSON.stringify(result[0]));
    let address = result[0];

    let alert1 = this.alertCtrl.create();
    alert1.setTitle('Subscribe to Location');
       
    
      if (address.subLocality) {
        this.subLocality = address.subLocality;
        console.log(this.subLocality);
        alert1.addInput({
          type: 'radio',
          label: this.subLocality,
          value: this.subLocality          
      });
      }

      if (address.locality) {
        this.locality = address.locality;
        console.log(this.locality);
        alert1.addInput({
          type: 'radio',
          label: this.locality,
          value: this.locality          
      });
      }
      if (address.subAdministrativeArea) {
        this.subAdminArea = address.subAdministrativeArea;
        console.log(this.subAdminArea);
        alert1.addInput({
          type: 'radio',
          label: this.subAdminArea,
          value: this.subAdminArea         
      });
      }
      if (address.administrativeArea) {
        this.adminArea= address.administrativeArea;
        console.log(this.adminArea);
        alert1.addInput({
          type: 'radio',
          label: this.adminArea,
          value: this.adminArea          
      });
      }

      
      alert1.addButton({
        text: 'Cancel',
        handler: data => {
          console.log("cancel clicked");
        }
    });
    alert1.addButton({
      text: 'Subscribe',
      handler: data => {
        console.log("subscribe clicked" + JSON.stringify(data));
        this.push.load(data);
      }
  });

    alert1.present(); 

  }    
  )
  .catch((error: any) => console.log(error));
  
}

}
</code></pre>

Update the  IonicMobileApp/src/pages/watch-area/watch-area.module.ts as below

<pre><code>
import { NgModule } from '@angular/core';
import { IonicPageModule } from 'ionic-angular';
import { WatchAreaPage } from './watch-area';

@NgModule({
  declarations: [
    WatchAreaPage,
  ],
  imports: [
    IonicPageModule.forChild(WatchAreaPage),
  ],
})
export class WatchAreaPageModule {}
</code></pre>


#### 3.2.2 Add Pushservice Provider

When the user clicks on the 'Receive notification' button in the 'Watch Area' page 'Pushservice' provider method is called. This method checks if the user subscribed tag(location name) is present in the Mobile Foundation server by invoking the 'PushAPI' adapter. When the call is returned successful, the provider issues calls to register the device and subscribe to the tag in the same order.

Add provider by name 'pushservice' under IonicMobileApp/src/providers.
 
Update the IonicMobileApp/src/providers/pushservice/pushservice.ts with code below.

<pre><code>
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
</code></pre>



#### 3.2.3 Update Report New Page to send the push notification

When a user submits a new grievance in a location, a notification has to be sent to all the users who have subscribed to receive notification for issues on that location.

Update the uploadImage() call in the submit() method in the IonicMobileApp/src/pages/report-new/report-new.ts file with the code below
<pre><code>

 this.myWardDataProvider.uploadImage(imageFilename, this.capturedImage).then(
              (response) => {
                this.imageResizer.resize(this.getImageResizerOptions()).then(
                  (filePath: string) => {
                    this.myWardDataProvider.uploadImage(thumbnailImageFilename, filePath).then(
                      (response) => {
                        this.loader.dismiss();
                        this.showToast('Image Uploaded Successfully');
                        this.showAlert('Upload Successful', 'Successfully uploaded problem report to server', false, () => {
                          this.myWardDataProvider.data.push(grievance);
                          //send notification
                          this.sendNotification();
                          this.navCtrl.pop();
                        })
</code></pre>

Add the implementation for sendNotification() method in the same file

<pre><code>
sendNotification(){

    this.dataRequest = new WLResourceRequest("/adapters/PushAPI/sendMessage", WLResourceRequest.POST);
    this.dataRequest.setQueryParameter("tagnames", this.tagnames);
    this.dataRequest.setQueryParameter("description",this.description);
    this.dataRequest.send().then(
      (response) => {
        console.log('--> Notification sent');   
   
      }, (failure) => {
        console.log('--> Send notification failed\n', JSON.stringify(failure)); 
            
      })

  }

</code></pre>

With the above changes, please try to run the application following the readme instructions of this repository.