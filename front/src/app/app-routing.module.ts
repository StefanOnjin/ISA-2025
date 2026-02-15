import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login.component';
import { RegisterComponent } from './auth/register.component';
import { HomeComponent } from './home/home.component';
import { AuthGuard } from './services/auth.guard';
import { UserProfileComponent } from './profile/user-profile.component';

import { VideoUploadComponent } from './video/video-upload/video-upload.component';

import { VideoDetailComponent } from './video/video-detail/video-detail.component';
import { VideoMapComponent } from './video/video-map/video-map.component';
import { VideoPremieresComponent } from './video/video-premieres/video-premieres.component';
import { VideoPremierComponent } from './video/video-premier/video-premier.component';
import { WatchPartyRoomComponent } from './watch-party/watch-party-room.component';

const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'home', component: HomeComponent },
  { path: 'map', component: VideoMapComponent },
  { path: 'premieres', component: VideoPremieresComponent },
  { path: 'premiers/:id', component: VideoPremierComponent },
  { path: 'watch-party', component: WatchPartyRoomComponent, canActivate: [AuthGuard] },
  { path: 'upload', component: VideoUploadComponent, canActivate: [AuthGuard] },
  { path: 'videos/:id', component: VideoDetailComponent },
  { path: 'users/:username', component: UserProfileComponent },
  { path: '**', redirectTo: 'home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
