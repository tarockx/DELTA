package unipd.elia.deltacore.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.security.cert.X509Certificate;

import unipd.elia.deltacore.R;

public class CertificateDetailsFragment extends Fragment {

    private X509Certificate x509Certificate;
    public void setCertificate(X509Certificate x509Certificate){
        this.x509Certificate = x509Certificate;
    }

    public static CertificateDetailsFragment newInstance(X509Certificate x509Certificate) {
        CertificateDetailsFragment fragment = new CertificateDetailsFragment();
        fragment.setCertificate(x509Certificate);
        return fragment;
    }

    public CertificateDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_certificate_details, container, false);

        if(x509Certificate == null){
            mainView.findViewById(R.id.fragment_certificate_details_layoutCertificateDetails).setVisibility(View.GONE);
        }
        else {
            mainView.findViewById(R.id.fragment_certificate_details_layoutCertificateError).setVisibility(View.GONE);

            TextView txtSubjecdDN = (TextView) mainView.findViewById(R.id.fragment_certificate_details_txtSubjectDN);
            txtSubjecdDN.setText(x509Certificate.getSubjectDN().getName());

            TextView txtIssuerDN = (TextView) mainView.findViewById(R.id.fragment_certificate_details_txtIssuerDN);
            txtIssuerDN.setText(x509Certificate.getIssuerDN().getName());

            TextView txtNotAfter = (TextView) mainView.findViewById(R.id.fragment_certificate_details_txtNotAfter);
            txtNotAfter.setText(x509Certificate.getNotAfter().toString());
        }

        return mainView;
    }

}
