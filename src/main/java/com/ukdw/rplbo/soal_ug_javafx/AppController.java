package com.ukdw.rplbo.soal_ug_javafx;

import com.ukdw.rplbo.soal_ug_javafx.data.Mahasiswa_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Matakuliah_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Nilai_table;
import com.ukdw.rplbo.soal_ug_javafx.entity.Mahasiswa;
import com.ukdw.rplbo.soal_ug_javafx.entity.Matakuliah;
import com.ukdw.rplbo.soal_ug_javafx.entity.Nilai;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AppController {
    @FXML
    private ComboBox<String> option;
    @FXML
    private TableView<Object> table;
    @FXML
    private TableColumn<Object,String> column1;
    @FXML
    private TableColumn<Object,String> column2;
    @FXML
    private TableColumn<Object,String> column3;

    @FXML
    private BarChart<String, Number> barchart;
    @FXML
    private LineChart<String, Number> linechart;
    @FXML
    private PieChart piechart;


    Mahasiswa_table mhs_table = new Mahasiswa_table();
    Matakuliah_table mtkl_table = new Matakuliah_table();
    Nilai_table nilai_table = new Nilai_table();


    public AppController() throws SQLException {
    }

    @FXML
    public void initialize() throws SQLException {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Mahasiswa",
                "Matakuliah"
        );
        option.setItems(options);
        option.setValue("Mahasiswa");

        option.valueProperty().addListener((observable, oldValue, newValue) -> {
            table.getItems().clear();

            if ("Matakuliah".equals(newValue)) {
                linechart.setVisible(true);
                column1.setText("kode_mk");
                column1.setCellValueFactory(new PropertyValueFactory<>("kode_mk"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));


                column3.setText("sks");
                column3.setCellValueFactory(new PropertyValueFactory<>("sks"));

                table.setItems(FXCollections.observableArrayList(mtkl_table.fetch_all_matkul()));
            } else {
                linechart.setVisible(false);
                column1.setText("NIM");
                column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));

                column3.setText(" ");
                column3.setCellValueFactory(null);

                table.setItems(FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa()));
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {

                if (newSelection instanceof Mahasiswa) {
                    Mahasiswa m = (Mahasiswa) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getNIM() + ")");

                    // -- chart --
                    update_barchart("nim",m.getNIM());
                    update_piechart("nim",m.getNIM());


                } else if (newSelection instanceof Matakuliah) {
                    Matakuliah m = (Matakuliah) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getKode_mk() + ")");

                    // -- chart --
                    update_barchart("kode_mk",m.getKode_mk());
                    update_piechart("kode_mk",m.getKode_mk());
                    update_linechart(m.getKode_mk());
                }
            }
        });

        linechart.setVisible(false);
        column1.setText("NIM");
        column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
        column2.setText("nama");
        column2.setCellValueFactory(new PropertyValueFactory<>("nama"));
        column3.setText(" ");

        ObservableList<Object> data = FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa());
        table.setItems(data);

    }

    public void update_barchart(String target_col,String val) {
        // Membersihkan data chart sebelumnya
        barchart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Distribusi Nilai");

        // Menggunakan array untuk menghitung jumlah masing-masing grade sesuai urutan di nilai_table
        int[] counts = new int[nilai_table.penilaian.length];

        // Menggunakan method fetch_nilai_by yang sudah disediakan untuk efisiensi
        List<Nilai> listNilai = nilai_table.fetch_nilai_by(target_col, val);

        for (Nilai n : listNilai) {
            String grade = n.getNilai();
            for (int i = 0; i < nilai_table.penilaian.length; i++) {
                if (nilai_table.penilaian[i].equals(grade)) {
                    counts[i]++;
                    break;
                }
            }
        }

        // Memasukkan data ke dalam chart
        for (int i = 0; i < nilai_table.penilaian.length; i++) {
            series.getData().add(new XYChart.Data<>(nilai_table.penilaian[i], counts[i]));
        }

        barchart.getData().add(series);
    }

    public void update_linechart(String kode_mk) {
        linechart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rata-rata Nilai");

        java.util.HashMap<Integer, Double> totalScore = new java.util.HashMap<>();
        java.util.HashMap<Integer, Integer> countScore = new java.util.HashMap<>();

        // Menggunakan method fetch_nilai_by_kode_mk
        List<Nilai> listNilai = nilai_table.fetch_nilai_by_kode_mk(kode_mk);

        for (Nilai n : listNilai) {
            // Fetch entity mahasiswa
            Mahasiswa mhs = mhs_table.fetch_mahasiswa_by_nim(n.getNIM());
            if (mhs != null) {
                int angkatan = mhs.getAngkatan();
                // Menggunakan method get_converted_nilai() dari entity Nilai
                double numGrade = n.get_converted_nilai();

                totalScore.put(angkatan, totalScore.getOrDefault(angkatan, 0.0) + numGrade);
                countScore.put(angkatan, countScore.getOrDefault(angkatan, 0) + 1);
            }
        }

        // Mengurutkan angkatan agar garis pada chart rapi
        java.util.List<Integer> sortedAngkatan = new java.util.ArrayList<>(totalScore.keySet());
        java.util.Collections.sort(sortedAngkatan);

        // Memasukkan data mean ke dalam chart
        for (int angkatan : sortedAngkatan) {
            double mean = totalScore.get(angkatan) / countScore.get(angkatan);
            series.getData().add(new XYChart.Data<>(String.valueOf(angkatan), mean));
        }

        linechart.getData().add(series);
    }

    public void update_piechart(String target_col, String val) {
        piechart.getData().clear();

        int[] counts = new int[nilai_table.penilaian.length];
        List<Nilai> listNilai = nilai_table.fetch_nilai_by(target_col, val);

        for (Nilai n : listNilai) {
            String grade = n.getNilai();
            for (int i = 0; i < nilai_table.penilaian.length; i++) {
                if (nilai_table.penilaian[i].equals(grade)) {
                    counts[i]++;
                    break;
                }
            }
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (int i = 0; i < nilai_table.penilaian.length; i++) {
            if (counts[i] > 0) {
                // Menambahkan label beserta jumlah di piechart sesuai contoh soal
                pieChartData.add(new PieChart.Data(nilai_table.penilaian[i] + " (" + counts[i] + ")", counts[i]));
            }
        }

        piechart.setData(pieChartData);
    }
}