package com.invpeajes.ui.common;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingWorker;

/**
 * Ejecuta una consulta a la base de datos en segundo plano para que la ventana
 * no se congele, y devuelve el resultado (o el error) en el hilo de la interfaz.
 *
 * @author Luis Guerrero
 * @version 1.0
 */
public final class Async {

    private Async() {
    }

    /**
     * @param task Trabajo a ejecutar fuera del hilo de la interfaz (ej. llamar a un Service).
     * @param onSuccess Qué hacer con el resultado, en el hilo de la interfaz.
     * @param onError Qué hacer si el trabajo lanza una excepción, en el hilo de la interfaz.
     * @param <T> Tipo del resultado.
     */
    public static <T> void run(Supplier<T> task, Consumer<T> onSuccess, Consumer<RuntimeException> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return task.get();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    onError.accept(cause instanceof RuntimeException
                            ? (RuntimeException) cause
                            : new IllegalStateException("Error inesperado: " + cause.getMessage(), cause));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
