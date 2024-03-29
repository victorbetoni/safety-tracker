package com.safetytracker.api.analysis;

import com.safetytracker.api.registry.ProvinceRegistry;
import com.safetytracker.api.registry.Tuple;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BRAnalysis {

    private int br;
    private String province;
    private int accidentCount = 0;
    private int fatalAccidents = 0;
    private int feridoAccidents = 0;
    private int ilesoAccidents = 0;
    private HashMap<Integer, Integer> acidentByDaytime = new HashMap<>();
    private HashMap<String, Integer> accidentTypeQuantity = new HashMap<>();
    private Map<Integer, HashMap<String, Integer>> accidentTypeByDaytime = new HashMap<>();
    private HashMap<String, Integer> accidentCausesQuantity = new HashMap<>();

    public Tuple<String, Integer> getAcidenteCausaMaisRecorrente() {
        final int[] qtd = {0};
        AtomicReference<String> maisRecorrente = new AtomicReference<>();
        accidentCausesQuantity.forEach((x,y) -> {
            if(y > qtd[0]) {
                qtd[0] = y;
                maisRecorrente.set(x);
            }
        });

        return new Tuple<String, Integer>(maisRecorrente.get(), qtd[0]);
    }

    public Tuple<String, Integer> getAcidenteMaisRecorrenteEmHorario(int time) {
        Map<String, Integer> tipos = accidentTypeByDaytime.get(time);
        final int[] qtd = {0};
        AtomicReference<String> maisRecorrente = new AtomicReference<>();
        tipos.forEach((x,y) -> {
            if(y > qtd[0]) {
                qtd[0] = y;
                maisRecorrente.set(x);
            }
        });

        return new Tuple<>(maisRecorrente.get(), qtd[0]);
    }

    public Integer getHorarioMaisPerigoso() {
        int dangerous = 0;
        int hour = 0;
        for(int i = 0; i < 24; i++) {
            if(acidentByDaytime.get(i) > dangerous) {
                dangerous = acidentByDaytime.get(i);
                hour = i;
            }
        }
        return hour;
    }

    public Integer getHorarioMaisSeguro() {
        int safe = -989;
        int hour = 0;
        for(int i = 0; i < 24; i++) {
            if(acidentByDaytime.get(i) > safe) {
                if(safe == -989) {
                    safe = acidentByDaytime.get(i);
                    hour = i;
                    continue;
                }
                if(safe > acidentByDaytime.get(i)) {
                    safe = acidentByDaytime.get(i);
                    hour = i;
                }
            }
        }
        return hour;
    }

    public Tuple<String, Integer> getTipoMaisFrequente() {
        final int[] qtd = {0};
        AtomicReference<String> maisRecorrente = new AtomicReference<>();
        accidentTypeQuantity.forEach((x,y) -> {
            if(y > qtd[0]) {
                qtd[0] = y;
                maisRecorrente.set(x);
            }
        });

        return new Tuple<>(maisRecorrente.get(), qtd[0]);
    }

    public BRAnalysis(int br, String province) {
        this.br = br;
        this.province = province;
        for(int i = 0; i < 24; i++) {
            acidentByDaytime.put(i, 0);
            accidentTypeByDaytime.put(i, new HashMap<>());
        }
    }

    public int getFatalAccidents() {
        return fatalAccidents;
    }

    public int getFeridoAccidents() {
        return feridoAccidents;
    }

    public int getIlesoAccidents() {
        return ilesoAccidents;
    }

    public int getAccidentCount() {
        return accidentCount;
    }

    public void load() {
        System.out.println("Loading specific data for province " + province + "" + br);
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("datasets/" + province + ".arff").getInputStream()))) {
                String line;
                while((line = reader.readLine()) != null) {
                    if(line.startsWith("@") || line.isEmpty()) {
                        continue;
                    }
                    String[] data = line.split(",");
                    if(Integer.parseInt(data[2]) != this.br) {
                        continue;
                    }

                    int horario = Integer.parseInt(data[1]);
                    String cause = data[4];
                    String type = data[5];
                    String classification = data[6];

                    accidentCount++;

                    if(classification.equalsIgnoreCase("COM_VITIMAS_FERIDAS")) feridoAccidents++;
                    if(classification.equalsIgnoreCase("SEM_VITIMAS")) ilesoAccidents++;
                    if(classification.equalsIgnoreCase("COM_VITIMAS_FATAIS")) fatalAccidents++;

                    int acidentesCount = acidentByDaytime.get(horario);
                    acidentesCount++;
                    acidentByDaytime.remove(horario);
                    acidentByDaytime.put(horario, acidentesCount);

                    if(!accidentTypeQuantity.containsKey(type)) {
                        accidentTypeQuantity.put(type, 1);
                    } else {
                        int accidentTypeCount = accidentTypeQuantity.get(type);
                        accidentTypeCount++;
                        accidentTypeQuantity.remove(type);
                        accidentTypeQuantity.put(type, accidentTypeCount);
                    }

                    if(!accidentCausesQuantity.containsKey(cause)) {
                        accidentCausesQuantity.put(cause, 1);
                    } else {
                        int accidentCauseCount = accidentCausesQuantity.get(cause);
                        accidentCauseCount++;
                        accidentCausesQuantity.remove(cause);
                        accidentCausesQuantity.put(cause, accidentCauseCount);
                    }

                    HashMap<String, Integer> causasNoHorario = accidentTypeByDaytime.get(horario);
                    if(!causasNoHorario.containsKey(cause)) {
                        causasNoHorario.put(cause, 1);
                    } else {
                        int value = causasNoHorario.get(cause);
                        value++;
                        causasNoHorario.remove(cause);
                        causasNoHorario.put(cause, value);
                    }
                    accidentTypeByDaytime.remove(horario);
                    accidentTypeByDaytime.put(horario, causasNoHorario);


                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
    }

    public static class ValueComparator<T> implements Comparator<T> {
        Map<T, Integer> base;

        public ValueComparator(Map<T, Integer> base) {
            this.base = base;
        }

        public int compare(T a, T b) {
            if (base.get(a) <= base.get(b)) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
