import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './auth/login.component';
import { RegisterComponent } from './auth/register.component';
import { HomeComponent } from './home/home.component';
import { VideoUploadComponent } from './video/video-upload/video-upload.component';
import { VideoListComponent } from './video/video-list/video-list.component';
import { VideoDetailComponent } from './video/video-detail/video-detail.component';
import { UserProfileComponent } from './profile/user-profile.component';
import { VideoMapComponent } from './video/video-map/video-map.component';
import { VideoPremieresComponent } from './video/video-premieres/video-premieres.component';
import { VideoPremierComponent } from './video/video-premier/video-premier.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    HomeComponent,
    VideoUploadComponent,
    VideoListComponent,
    VideoDetailComponent,
    UserProfileComponent,
    VideoMapComponent,
    VideoPremieresComponent,
    VideoPremierComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule, 
    ReactiveFormsModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
