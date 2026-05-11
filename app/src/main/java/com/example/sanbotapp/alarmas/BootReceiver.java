import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.alarmas.AlarmScheduler;

import java.util.List;

// Las alarmas se pierden si el teléfono se apaga; este receiver las reprograma
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        ActividadRepository repo = new ActividadRepository(context);
        List<Actividad> pendientes = repo.getDeHoy(); // o getTodasPendientes()

        for (Actividad a : pendientes) {
            if (a.getEstado().equals(Actividad.ESTADO_PENDIENTE)) {
                AlarmScheduler.programarActividad(context, a);
            }
        }
    }
}