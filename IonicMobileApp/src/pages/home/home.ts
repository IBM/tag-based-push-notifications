/**
 * Copyright 2017 IBM Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component } from '@angular/core';
import { NavController, NavParams, LoadingController } from 'ionic-angular';
import { ImgCacheService } from 'ng-imgcache';
import { MyWardDataProvider } from '../../providers/my-ward-data/my-ward-data';
import { AuthHandlerProvider } from '../../providers/auth-handler/auth-handler';
import { ProblemDetailPage } from '../problem-detail/problem-detail';
import { ReportNewPage } from '../report-new/report-new';
import { LoginPage } from '../login/login';
import { WatchAreaPage } from '../watch-area/watch-area';
import { Geolocation,GeolocationOptions } from '@ionic-native/geolocation';


@Component({
  selector: 'page-home',
  templateUrl: 'home.html'
})
export class HomePage {
  loader: any;
  grievances: any;
  objectStorageAccess: any;
  options : GeolocationOptions;
  lat: any;
  lng: any;
  


  constructor(public navCtrl: NavController, public loadingCtrl: LoadingController,
    public myWardDataProvider: MyWardDataProvider, public imgCache: ImgCacheService,
    private authHandler:AuthHandlerProvider,private geolocation: Geolocation,public navParams: NavParams){
    
    this.lat = navParams.get('lat');
    this.lng = navParams.get('lng'); 
    console.log('--> HomePage constructor() called');
  }

  ionViewDidLoad() {
    console.log('--> HomePage ionViewDidLoad() called');
    
    this.loadData();
  }

  ionViewWillEnter() {
    console.log('--> HomePage ionViewWillEnter() called');
    this.initAuthChallengeHandler();
  }

  loadData() {
    this.loader = this.loadingCtrl.create({
      content: 'Loading data. Please wait ...',
    });
    this.loader.present().then(() => { 
     
        this.options = {
          enableHighAccuracy : true
        };
        if( this.lat && this.lng )
        {
          this.myWardDataProvider.lat = this.lat;
          console.log('if lat cur='+this.myWardDataProvider.lat);      
          this.myWardDataProvider.lng = this.lng;          
          console.log('if long cur='+this.myWardDataProvider.lng); 
          this.invokeLoad();

        }
        else{
        this.geolocation.getCurrentPosition(this.options).then((resp) => {
          this.myWardDataProvider.lat = resp.coords.latitude;
          console.log('else lat cur='+this.myWardDataProvider.lat);      
          this.myWardDataProvider.lng = resp.coords.longitude;          
          console.log('else long cur='+this.myWardDataProvider.lng); 
          this.invokeLoad();
            //  });
      }); 
    }
  });
}

invokeLoad(){

this.myWardDataProvider.load().then(data => {
  this.myWardDataProvider.getObjectStorageAccess().then(objectStorageAccess => {
    this.objectStorageAccess = objectStorageAccess;
    this.imgCache.init({
      headers: {
        'Authorization': this.objectStorageAccess.authorizationHeader
      }
    }).then( () => {
      console.log('--> HomePage initialized imgCache');
      this.loader.dismiss();
      this.grievances = data;
    });
  });
});
}

  // https://www.joshmorony.com/a-simple-guide-to-navigation-in-ionic-2/
  itemClick(grievance) {
    this.navCtrl.push(ProblemDetailPage, { grievance: grievance, baseUrl: this.objectStorageAccess.baseUrl });
  }

  reportNewProblem(){
    this.navCtrl.push(ReportNewPage);
  }

  logout(){

   
    this.authHandler.logout();
    this.navCtrl.setRoot(LoginPage);
    
    
  }     

  refresh() {
    this.myWardDataProvider.data = null;
    this.loadData();
  }
 

  initAuthChallengeHandler() {
    this.authHandler.setHandleChallengeCallback(() => {
      this.loader.dismiss();
      this.navCtrl.push(LoginPage, { isPushed: true });
    });
    this.authHandler.setLoginSuccessCallback(() => {
      let view = this.navCtrl.getActive();
      if (view.instance instanceof LoginPage) {
        this.navCtrl.pop().then(() =>{
          this.loader = this.loadingCtrl.create({
            content: 'Loading data. Please wait ...'
          });
          this.loader.present();
        });
      }
    });
  }

  watchProblems(){
  
    this.navCtrl.push(WatchAreaPage);
   
  }


}
