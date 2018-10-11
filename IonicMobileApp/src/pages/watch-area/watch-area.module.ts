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
