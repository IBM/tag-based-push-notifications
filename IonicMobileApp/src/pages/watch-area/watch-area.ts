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
    let prevLoc = new LatLng(13.0715, 80.241516);
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
