import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { PaieCalculComponent } from './components/paie-calcul/paie-calcul.component';
import { PaieDetailComponent } from './components/paie-detail/paie-detail.component';
import { PointageAjoutComponent } from './components/pointage-ajout/pointage-ajout.component';
import { PointageAnomaliesComponent } from './components/pointage-anomalies/pointage-anomalies.component';

const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'pointage', component: PointageAjoutComponent },
  { path: 'anomalies', component: PointageAnomaliesComponent },
  { path: 'paie', component: PaieCalculComponent },
  { path: 'paie/detail/:employeeId', component: PaieDetailComponent },
  { path: '**', redirectTo: '/home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
