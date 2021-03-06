package org.tosl.coronawarncompanion.dkdownload;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.tosl.coronawarncompanion.diagnosiskeys.DiagnosisKeysImport;
import org.tosl.coronawarncompanion.diagnosiskeys.DiagnosisKeysProtos;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;

import static org.tosl.coronawarncompanion.dkdownload.Unzip.getUnzippedBytesFromZipFileBytes;

public class DKDownloadUtils {

    private static final String TAG = "DKDownloadUtils";

    public static Single<List<DiagnosisKeysProtos.TemporaryExposureKey>> getDKsForCountries(Context context, RequestQueue queue, Date minDate, List<DKDownloadCountry> countries) {
        List<Single<List<URL>>> singleList = countries.stream().map(dkDownloadCountry -> dkDownloadCountry.getUrls(queue, minDate)).collect(Collectors.toList());
        return Single.zip(singleList, results -> {
            List<URL> urlList = new ArrayList<>();
            for (Object result : results) {
                urlList.addAll((List<URL>) result);
            }
            return urlList;
        }).flatMap(urls -> processUrlList(context, queue, urls));
    }

    public static List<DiagnosisKeysProtos.TemporaryExposureKey> parseBytesToTeks(Context context, byte[] fileBytes) {
        byte[] exportDotBinBytes = {};
        try {
            exportDotBinBytes = getUnzippedBytesFromZipFileBytes(fileBytes, "export.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }

        DiagnosisKeysImport diagnosisKeysImport = new DiagnosisKeysImport(exportDotBinBytes, context);
        List<DiagnosisKeysProtos.TemporaryExposureKey> dkList = diagnosisKeysImport.getDiagnosisKeys();
        if (dkList != null) {
            Log.d(TAG, "Number of keys in this file: " + dkList.size());
            return dkList;
        }
        return new ArrayList<>();
    }

    private static Single<List<DiagnosisKeysProtos.TemporaryExposureKey>> processUrlList(Context context, RequestQueue queue, List<URL> diagnosisKeysUrls) {
        Subject<List<DiagnosisKeysProtos.TemporaryExposureKey>> diagnosisKeysSubject = ReplaySubject.create();

        for (URL url: diagnosisKeysUrls) {
            Log.d(TAG, "Going to download: " + url);
            ByteArrayRequest byteArrayRequest = new ByteArrayRequest(
                    Request.Method.GET,
                    url.toString(),
                    fileBytes -> {
                        if (fileBytes.length == 0) {
                            Log.d(TAG, "Download resulted in 0 bytes: " + url);
                            diagnosisKeysSubject.onNext(new ArrayList<>());
                        } else {
                            Log.d(TAG, "Download complete: " + url);
                            diagnosisKeysSubject.onNext(parseBytesToTeks(context, fileBytes));
                        }
                    },
                    diagnosisKeysSubject::onError);
            queue.add(byteArrayRequest);
        }
        return diagnosisKeysSubject.take(diagnosisKeysUrls.size()).doFinally(diagnosisKeysSubject::onComplete).reduce(new ArrayList<>(), (accumulated, current) -> {
            accumulated.addAll(current);
            return accumulated;
        });
    }
}
