package com.infirmary.backend.configuration.impl;

import static com.infirmary.backend.shared.utility.FunctionUtil.createSuccessResponse;
import static com.infirmary.backend.shared.utility.FunctionUtil.isValidPassword;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.infirmary.backend.configuration.Exception.UserAlreadyExists;
import com.infirmary.backend.configuration.dto.AdDTO;
import com.infirmary.backend.configuration.dto.DoctorDTO;
import com.infirmary.backend.configuration.dto.JwtResponse;
import com.infirmary.backend.configuration.dto.LoginRequestDTO;
import com.infirmary.backend.configuration.dto.PasswordChangeDTO;
import com.infirmary.backend.configuration.dto.PatientReqDTO;
import com.infirmary.backend.configuration.jwt.JwtUtils;
import com.infirmary.backend.configuration.model.AD;
import com.infirmary.backend.configuration.model.Conformation;
import com.infirmary.backend.configuration.model.Doctor;
import com.infirmary.backend.configuration.model.Location;
import com.infirmary.backend.configuration.model.PasswordChange;
import com.infirmary.backend.configuration.model.Patient;
import com.infirmary.backend.configuration.repository.AdRepository;
import com.infirmary.backend.configuration.repository.ConformationRepository;
import com.infirmary.backend.configuration.repository.DoctorRepository;
import com.infirmary.backend.configuration.repository.LocationRepository;
import com.infirmary.backend.configuration.repository.PasswordChangeRepository;
import com.infirmary.backend.configuration.repository.PatientRepository;
import com.infirmary.backend.configuration.securityimpl.UserDetailsImpl;
import com.infirmary.backend.configuration.service.AuthService;
import com.infirmary.backend.shared.utility.FunctionUtil;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService{
    
    private AuthenticationManager authenticationManager;
    private PatientRepository patientRepository;
    private PasswordEncoder encoder;
    private JwtUtils jwtUtils;
    private AdRepository adRepository;
    private DoctorRepository doctorRepository;
    private LocationRepository locationRepository;
    private ConformationRepository conformationRepository;
    private JavaMailSender javaMailSender;
    private PasswordChangeRepository passwordChangeRepository;
    
    @Override
    public ResponseEntity<?> loginServicePat(LoginRequestDTO loginRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.genrateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList());
        if(!roles.get(0).equals("ROLE_PATIENT")) throw new SecurityException("Unauthorized Not Patient");
        if(conformationRepository.existsByPatient_Email(loginRequestDTO.getEmail())) throw new IllegalArgumentException("Please Verify Email");
        return ResponseEntity.ok(new JwtResponse(jwt,userDetails.getUsername(),roles));
    }
    
    @Override
    public ResponseEntity<?> signUpPat(PatientReqDTO patientDTO) throws UserAlreadyExists, IOException, MessagingException {
            if(patientRepository.existsByEmail(patientDTO.getEmail())){
                throw new UserAlreadyExists("User Already Exists");
            }

            if(!patientDTO.getSchool().equals("Guest")){
                if(patientRepository.existsBySapId(patientDTO.getSapID())){
                    throw new UserAlreadyExists("User With SapId Already Exists");
                }
            }

            if(!(isValidPassword(patientDTO.getPassword()))) throw new IllegalArgumentException("Password must satisfy conditions");

            patientDTO.setPassword(encoder.encode(patientDTO.getPassword()));
    
            Patient patient = new Patient(
                patientDTO
            );

            if(patientDTO.getImg() != null){

                byte[] data = Base64.getDecoder().decode(patientDTO.getImg().substring(patientDTO.getImg().indexOf(",")+1));
                
                String type = patientDTO.getImg().split(";")[0].split("/")[1];
                
                String fileName = patientDTO.getEmail().replace(".", "_");
                
                String filePath = "./build/resources/main/static/Profile/"+fileName+"."+type;
                String flpth = "./src/main/resources/static/Profile/"+fileName+"."+type;
                
                try{
                    FileUtils.writeByteArrayToFile(new File(filePath), data);
                    FileUtils.writeByteArrayToFile(new File(flpth), data);
                }catch (Exception e){
                    throw new IOException(e);
                }
                
                patient.setImageUrl("Profile/"+fileName+"."+type);
            }

            patient = patientRepository.save(patient);

            Conformation conformation = new Conformation();
            conformation.setPatient(patient);
            conformation.setTimestamp(System.currentTimeMillis());

            conformation = conformationRepository.save(conformation);

            String toAddress = patient.getEmail();
            String fromAddress = "infirmarytest@gmail.com";
            String senderName = "UPES UHS";
            String subject = "Please verify your registration";
            String content = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Verify Your Email - UPES UHS</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f4f4; font-family:'Segoe UI', Roboto, sans-serif; color:#333;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);">
          <tr>
            <td align="center" bgcolor="#4f46e5" style="padding:40px 20px;">
              <h1 style="margin:0; color:#ffffff;">Welcome to UPES UHS!</h1>
              <p style="margin:8px 0 0; color:#e0e0e0;">Your health, our priority.</p>
            </td>
          </tr>
          <tr>
            <td style="padding:30px;">
              <p style="font-size:16px;">Hello <strong>[[name]]</strong>,</p>
              <p style="font-size:16px; line-height:1.5;">
                Thank you for registering with <strong>UPES UHS</strong>. Please verify your email address to complete your registration.
              </p>
              <p style="text-align:center; margin:30px 0;">
                <a href="[[URL]]" style="background-color:#4f46e5; color:#ffffff; padding:14px 28px; text-decoration:none; border-radius:6px; display:inline-block; font-size:16px;">
                  Verify My Email
                </a>
              </p>
              <p style="font-size:14px; color:#888;">
                If you did not sign up for this account, you can safely ignore this email.
              </p>
            </td>
          </tr>
          <tr>
            <td bgcolor="#f4f4f4" style="padding:20px; text-align:center; font-size:12px; color:#999;">
              &copy; 2025 UPES UHS. All rights reserved.<br/>
              UPES University, Bidholi, Dehradun, Uttarakhand 248007<br/>
              
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";


            
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            
            content = content.replace("[[name]]", patient.getName());
            String verifyURL = "https://uhs-backend.onrender.com/api/auth/user" + "/verify?code=" + conformation.getConformationToken();
            
            content = content.replace("[[URL]]", verifyURL);
            
            helper.setText(content, true);
            
            javaMailSender.send(message);

            return createSuccessResponse("Patient Created, Please verify email by clicking on the link sent to your email");
        }

        @Override
        public ResponseEntity<?> signUpDat(DoctorDTO doctorDTO) throws UserAlreadyExists, MessagingException, UnsupportedEncodingException{

            if(doctorRepository.existsByDoctorEmail(doctorDTO.getDoctorEmail())){
                throw new UserAlreadyExists("Doctor Already Exists");
            }

            if(!(isValidPassword(doctorDTO.getPassword()))) throw new IllegalArgumentException("Password must satisfy conditions");

            doctorDTO.setPassword(encoder.encode(doctorDTO.getPassword()));

            Doctor doctor = new Doctor(
                doctorDTO
            );

            doctor = doctorRepository.save(doctor);

            Conformation conformation = new Conformation();
            conformation.setDoctor(doctor);
            conformation.setTimestamp(System.currentTimeMillis());

            conformation = conformationRepository.save(conformation);

            String toAddress = doctor.getDoctorEmail();
            String fromAddress = "infirmarytest@gmail.com";
            String senderName = "UPES UHS";
            String subject = "Please verify your registration";
            String content = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Verify Your Email - UPES UHS</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f4f4; font-family:'Segoe UI', Roboto, sans-serif; color:#333;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);">
          <tr>
            <td align="center" bgcolor="#4f46e5" style="padding:40px 20px;">
              <h1 style="margin:0; color:#ffffff;">Welcome to UPES UHS!</h1>
              <p style="margin:8px 0 0; color:#e0e0e0;">Your health, our priority.</p>
            </td>
          </tr>
          <tr>
            <td style="padding:30px;">
              <p style="font-size:16px;">Hello <strong>[[name]]</strong>,</p>
              <p style="font-size:16px; line-height:1.5;">
                Thank you for registering with <strong>UPES UHS</strong>. Please verify your email address to complete your registration.
              </p>
              <p style="text-align:center; margin:30px 0;">
                <a href="[[URL]]" style="background-color:#4f46e5; color:#ffffff; padding:14px 28px; text-decoration:none; border-radius:6px; display:inline-block; font-size:16px;">
                  Verify My Email
                </a>
              </p>
              <p style="font-size:14px; color:#888;">
                If you did not sign up for this account, you can safely ignore this email.
              </p>
            </td>
          </tr>
          <tr>
            <td bgcolor="#f4f4f4" style="padding:20px; text-align:center; font-size:12px; color:#999;">
              &copy; 2025 UPES UHS. All rights reserved.<br/>
              UPES University, Bidholi, Dehradun, Uttarakhand 248007<br/>
              
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";


            
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            
            content = content.replace("[[name]]", doctor.getName());
            String verifyURL = "https://uhs-backend.onrender.com/api/auth/user" + "/verify?code=" + conformation.getConformationToken();
            
            content = content.replace("[[URL]]", verifyURL);
            
            helper.setText(content, true);
            
            javaMailSender.send(message);

            return createSuccessResponse("Doctor Created Please verify email by clicking on the link sent to the email");
        }

        @Override
        public ResponseEntity<?> signUpAD(AdDTO adDTO) throws UserAlreadyExists, UnsupportedEncodingException, MessagingException{
            if(adRepository.existsById(adDTO.getEmail())){
                throw new UserAlreadyExists("AD Already Exists");
            }

            if(!(isValidPassword(adDTO.getPassword()))) throw new IllegalArgumentException("Password must satisfy conditions");

            adDTO.setPassword(encoder.encode(adDTO.getPassword()));

            AD ad = new AD(adDTO);

            ad = adRepository.save(ad);

            Conformation conformation = new Conformation();
            conformation.setAd(ad);
            conformation.setTimestamp(System.currentTimeMillis());

            conformation = conformationRepository.save(conformation);

            String toAddress = ad.getAdEmail();
            String fromAddress = "infirmarytest@gmail.com";
            String senderName = "UPES UHS";
            String subject = "Please verify your registration";
            String content = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Verify Your Email - UPES UHS</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f4f4; font-family:'Segoe UI', Roboto, sans-serif; color:#333;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);">
          <tr>
            <td align="center" bgcolor="#4f46e5" style="padding:40px 20px;">
              <h1 style="margin:0; color:#ffffff;">Welcome to UPES UHS!</h1>
              <p style="margin:8px 0 0; color:#e0e0e0;">Your health, our priority.</p>
            </td>
          </tr>
          <tr>
            <td style="padding:30px;">
              <p style="font-size:16px;">Hello <strong>[[name]]</strong>,</p>
              <p style="font-size:16px; line-height:1.5;">
                Thank you for registering with <strong>UPES UHS</strong>. Please verify your email address to complete your registration.
              </p>
              <p style="text-align:center; margin:30px 0;">
                <a href="[[URL]]" style="background-color:#4f46e5; color:#ffffff; padding:14px 28px; text-decoration:none; border-radius:6px; display:inline-block; font-size:16px;">
                  Verify My Email
                </a>
              </p>
              <p style="font-size:14px; color:#888;">
                If you did not sign up for this account, you can safely ignore this email.
              </p>
            </td>
          </tr>
          <tr>
            <td bgcolor="#f4f4f4" style="padding:20px; text-align:center; font-size:12px; color:#999;">
              &copy; 2025 UPES UHS. All rights reserved.<br/>
              UPES University, Bidholi, Dehradun, Uttarakhand 248007<br/>
              
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";

            
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            
            content = content.replace("[[name]]", ad.getName());
            String verifyURL = "https://uhs-backend.onrender.com/api/auth/user" + "/verify?code=" + conformation.getConformationToken();
            
            content = content.replace("[[URL]]", verifyURL);
            
            helper.setText(content, true);
            
            javaMailSender.send(message);

            return createSuccessResponse("AD created Please verify email by clicking on the link sent to the email");
        }

        @Override
        public ResponseEntity<?> loginServiceAd(LoginRequestDTO loginRequestDTO,Double latitude, Double longitude) {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.genrateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList());

            if(!roles.get(0).equals("ROLE_AD")) throw new SecurityException("Unauthorized Not AD");

            Location presentLocation = null;

            List<Location> locations = locationRepository.findAll();

            for(Location location:locations){
                if(FunctionUtil.IsWithinRadius(location.getLatitude(), location.getLongitude(), latitude, longitude)){
                    presentLocation = location;
                    break;
                }
            }

            if(presentLocation == null) throw new IllegalArgumentException("Be Available at the Infirmary before Logging In");

            AD ad = adRepository.findByAdEmail(loginRequestDTO.getEmail()).orElseThrow(()->new ResourceNotFoundException("No User Found"));

            if(conformationRepository.existsByAd_AdEmail(loginRequestDTO.getEmail())) throw new IllegalArgumentException("Please Verify Email");

            ad.setLocation(presentLocation);

            adRepository.save(ad);

            return ResponseEntity.ok(new JwtResponse(jwt,userDetails.getUsername(),roles));
        }

        @Override
        public ResponseEntity<?> loginServiceDat(LoginRequestDTO loginRequestDTO) {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.genrateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList());

            if(!roles.get(0).equals("ROLE_DOCTOR")) throw new SecurityException("Unauthorized Not A doctor");

            if(conformationRepository.existsByDoctor_DoctorEmail(loginRequestDTO.getEmail())) throw new IllegalArgumentException("Please Verify Email");

            return ResponseEntity.ok(new JwtResponse(jwt,userDetails.getUsername(),roles));
        }

        @Override
        public ResponseEntity<?> loginServiceAdmin(LoginRequestDTO loginRequestDTO){
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(),loginRequestDTO.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.genrateJwtToken(authentication);

            UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetailsImpl.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList());

            if(!roles.get(0).equals("ROLE_ADMIN")) throw new SecurityException("Unauthorized Not a Admin");

            return createSuccessResponse(new JwtResponse(jwt,userDetailsImpl.getUsername(),roles));

        }

        @Override
public String verifyUser(UUID code) {
    Conformation conformation = conformationRepository.findById(code)
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification link."));

    // Delete conformation record to mark as verified
    conformationRepository.delete(conformation);

    String redirectUrl = "https://uhs.vercel.app";

String htmlContent = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email Verified</title>
    <style>
        body {
            font-family: 'Segoe UI', Roboto, Arial, sans-serif;
            background: linear-gradient(135deg, #6366f1 0%%, #4f46e5 100%%);
            margin: 0;
            padding: 0;
            color: #333;
        }
        .container {
            max-width: 600px;
            margin: 100px auto;
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 10px 20px rgba(0,0,0,0.1);
            text-align: center;
            padding: 40px 30px;
        }
        .success-icon {
            font-size: 80px;
            color: #4BB543;
            margin-bottom: 20px;
        }
        h1 {
            margin-bottom: 10px;
            font-size: 28px;
            color: #333;
        }
        p {
            font-size: 16px;
            color: #666;
        }
        .btn {
            display: inline-block;
            margin-top: 30px;
            padding: 14px 28px;
            font-size: 16px;
            background-color: #4f46e5;
            color: #fff;
            text-decoration: none;
            border-radius: 6px;
            box-shadow: 0 5px 10px 1px rgba(161, 156, 255, 0.5);
            transition: background-color 0.5s ease;
        }
        .btn:hover {
            background-color: #04021b;
        }
        .footer {
            margin-top: 40px;
            font-size: 12px;
            color: #aaa;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="success-icon">✅</div>
        <h1>Email Verified Successfully!</h1>
        <p>Thank you for verifying your email. Your account is now active and ready to use.</p>
        <a href="%s" class="btn">Go to Login</a>
        <div class="footer">
            &copy; 2025 UPES UHS. All rights reserved.
        </div>
    </div>
</body>
</html>
""".formatted(redirectUrl);


    return htmlContent;
}


        @Override
        public String forgetPassPat(String email) throws UnsupportedEncodingException, MessagingException {
            email = email.substring(0,email.indexOf("@")).concat(email.substring(email.indexOf("@")).replaceAll(",", "."));

            Patient patient = patientRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("No User Found"));
            Optional<PasswordChange> passRecord = passwordChangeRepository.findByPatient(patient);

            if(passRecord.isPresent()){
                passwordChangeRepository.delete(passRecord.get());
            }

            PasswordChange newPassChange = new PasswordChange();
            newPassChange.setPatient(patient);

            newPassChange = passwordChangeRepository.save(newPassChange);

            String toAddress = patient.getEmail();
            String fromAddress = "infirmarytest@gmail.com";
            String senderName = "UPES UHS";
            String subject = "Password Change";
            String content = """
            <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Reset Your Password - UPES UHS</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f4f4; font-family:'Segoe UI', Roboto, sans-serif; color:#333;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);">
          <tr>
            <td align="center" bgcolor="#4f46e5" style="padding:40px 20px;">
              <h1 style="margin:0; color:#ffffff;">Reset Your Password</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:30px;">
              <p style="font-size:16px;">Hello <strong>[[name]]</strong>,</p>
              <p style="font-size:16px; line-height:1.5;">
                You requested to reset your password. Click the button below to proceed.
              </p>
              <p style="text-align:center; margin:30px 0;">
                <a href="[[URL]]" style="background-color:#4f46e5; color:#ffffff; padding:14px 28px; text-decoration:none; border-radius:6px; display:inline-block; font-size:16px;">
                  Reset Password
                </a>
              </p>
              <p style="font-size:14px; color:#888;">
                If you did not request a password reset, please ignore this email or contact support.
              </p>
            </td>
          </tr>
          <tr>
            <td bgcolor="#f4f4f4" style="padding:20px; text-align:center; font-size:12px; color:#999;">
              &copy; 2025 UPES UHS. All rights reserved.<br/>
              UPES University, Bidholi, Dehradun, Uttarakhand 248007<br/>
              
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";
            
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            
            content = content.replace("[[name]]", patient.getName());
            String verifyURL = "https://uhs-backend.onrender.com/pass-change" + "?code=" + newPassChange.getChangeCode() + "&role=patient";
            
            content = content.replace("[[URL]]", verifyURL);
            
            helper.setText(content, true);
            
            javaMailSender.send(message);

            return "Please check the email for password change";
        }

        @Override
        public String forgetPassDoc(String email) throws UnsupportedEncodingException, MessagingException{

            email = email.substring(0,email.indexOf("@")).concat(email.substring(email.indexOf("@")).replaceAll(",", "."));


            Doctor doctor = doctorRepository.findByDoctorEmail(email).orElseThrow(()->new ResourceNotFoundException("No Doctor Found"));
            Optional<PasswordChange> passRecord = passwordChangeRepository.findByDoctor(doctor);

            if(passRecord.isPresent()){
                passwordChangeRepository.delete(passRecord.get());
            }

            PasswordChange newPassChange = new PasswordChange();
            newPassChange.setDoctor(doctor);

            newPassChange = passwordChangeRepository.save(newPassChange);

            String toAddress = doctor.getDoctorEmail();
            String fromAddress = "infirmarytest@gmail.com";
            String senderName = "UPES UHS";
            String subject = "Password Change";
            String content = """
            <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Reset Your Password - UPES UHS</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f4f4; font-family:'Segoe UI', Roboto, sans-serif; color:#333;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);">
          <tr>
            <td align="center" bgcolor="#4f46e5" style="padding:40px 20px;">
              <h1 style="margin:0; color:#ffffff;">Reset Your Password</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:30px;">
              <p style="font-size:16px;">Hello <strong>[[name]]</strong>,</p>
              <p style="font-size:16px; line-height:1.5;">
                You requested to reset your password. Click the button below to proceed.
              </p>
              <p style="text-align:center; margin:30px 0;">
                <a href="[[URL]]" style="background-color:#4f46e5; color:#ffffff; padding:14px 28px; text-decoration:none; border-radius:6px; display:inline-block; font-size:16px;">
                  Reset Password
                </a>
              </p>
              <p style="font-size:14px; color:#888;">
                If you did not request a password reset, please ignore this email or contact support.
              </p>
            </td>
          </tr>
          <tr>
            <td bgcolor="#f4f4f4" style="padding:20px; text-align:center; font-size:12px; color:#999;">
              &copy; 2025 UPES UHS. All rights reserved.<br/>
              UPES University, Bidholi, Dehradun, Uttarakhand 248007<br/>
              
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>

""";
            
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            
            content = content.replace("[[name]]", doctor.getName());
            String verifyURL = "https://uhs-backend.onrender.com/pass-change" + "?code=" + newPassChange.getChangeCode() + "&role=doctor";
            
            content = content.replace("[[URL]]", verifyURL);
            
            helper.setText(content, true);
            
            javaMailSender.send(message);

            return "Please check the email for password change";
        }

        @Override
        public String forgetPassAd(String email) throws UnsupportedEncodingException, MessagingException{
            
            email = email.substring(0,email.indexOf("@")).concat(email.substring(email.indexOf("@")).replaceAll(",", "."));

            
            AD ad = adRepository.findByAdEmail(email).orElseThrow(()->new ResourceNotFoundException("No AD Found"));
            Optional<PasswordChange> passRecord = passwordChangeRepository.findByAd(ad);

            if(passRecord.isPresent()){
                passwordChangeRepository.delete(passRecord.get());
            }

            PasswordChange newPassChange = new PasswordChange();
            newPassChange.setAd(ad);

            newPassChange = passwordChangeRepository.save(newPassChange);

            String toAddress = ad.getAdEmail();
            String fromAddress = "infirmarytest@gmail.com";
            String senderName = "UPES UHS";
            String subject = "Password Change";
            String content = """
            <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Reset Your Password - UPES UHS</title>
</head>
<body style="margin:0; padding:0; background-color:#f4f4f4; font-family:'Segoe UI', Roboto, sans-serif; color:#333;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
    <tr>
      <td align="center" style="padding: 40px 0;">
        <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.1);">
          <tr>
            <td align="center" bgcolor="#4f46e5" style="padding:40px 20px;">
              <h1 style="margin:0; color:#ffffff;">Reset Your Password</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:30px;">
              <p style="font-size:16px;">Hello <strong>[[name]]</strong>,</p>
              <p style="font-size:16px; line-height:1.5;">
                You requested to reset your password. Click the button below to proceed.
              </p>
              <p style="text-align:center; margin:30px 0;">
                <a href="[[URL]]" style="background-color:#4f46e5; color:#ffffff; padding:14px 28px; text-decoration:none; border-radius:6px; display:inline-block; font-size:16px;">
                  Reset Password
                </a>
              </p>
              <p style="font-size:14px; color:#888;">
                If you did not request a password reset, please ignore this email or contact support.
              </p>
            </td>
          </tr>
          <tr>
            <td bgcolor="#f4f4f4" style="padding:20px; text-align:center; font-size:12px; color:#999;">
              &copy; 2025 UPES UHS. All rights reserved.<br/>
              UPES University, Bidholi, Dehradun, Uttarakhand 248007<br/>
              
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";

            
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            
            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            
            content = content.replace("[[name]]", ad.getName());
            String verifyURL = "https://uhs-backend.onrender.com/pass-change" + "?code=" + newPassChange.getChangeCode() + "&role=ad";
            
            content = content.replace("[[URL]]", verifyURL);
            
            helper.setText(content, true);
            
            javaMailSender.send(message);

            return "Please check the email for password change";
        }

        @Override
        public String changePassPat(UUID code, PasswordChangeDTO passwordChangeDTO) throws UnsupportedEncodingException, MessagingException {
            PasswordChange passwordChange = passwordChangeRepository.findById(code).orElseThrow(()-> new ResourceNotFoundException("Invalid Code"));

            if(passwordChange.getPatient() == null) throw new IllegalArgumentException("Invalid Code");

            if(!(passwordChangeDTO.getNewPass().equals(passwordChangeDTO.getRepeatPassword()))) throw new IllegalArgumentException("Password does not match with repeat password");
            
            if(!(isValidPassword(passwordChangeDTO.getNewPass()))) throw new IllegalArgumentException("Pasword must contain atleast 8 characters including uppercase, lowercase, number and a special character");
            
            Patient patient = passwordChange.getPatient();
            patient.setPassword(encoder.encode(passwordChangeDTO.getNewPass()));
            patientRepository.save(patient);

            passwordChangeRepository.delete(passwordChange);

            return "Password Successfully changed";
        }

        @Override
        public String changePassDoc(UUID code, PasswordChangeDTO passwordChangeDTO) throws UnsupportedEncodingException, MessagingException {
            PasswordChange passwordChange = passwordChangeRepository.findById(code).orElseThrow(()-> new ResourceNotFoundException("Invalid Code"));

            if(passwordChange.getDoctor() == null) throw new IllegalArgumentException("Invalid Code");

            if(!(passwordChangeDTO.getNewPass().equals(passwordChangeDTO.getRepeatPassword()))) throw new IllegalArgumentException("Password does not match with repeat password");

            if(!(isValidPassword(passwordChangeDTO.getNewPass()))) throw new IllegalArgumentException("Pasword must contain atleast 8 characters including uppercase, lowercase, number and a special character");

            Doctor doctor = passwordChange.getDoctor();
            doctor.setPassword(encoder.encode(passwordChangeDTO.getNewPass()));
            doctorRepository.save(doctor);

            passwordChangeRepository.delete(passwordChange);

            return "Password Successfully changed";
        }

        @Override
        public String changePassAd(UUID code, PasswordChangeDTO passwordChangeDTO) throws UnsupportedEncodingException, MessagingException {
            PasswordChange passwordChange = passwordChangeRepository.findById(code).orElseThrow(()-> new ResourceNotFoundException("Invalid Code"));

            if(passwordChange.getAd() == null) throw new IllegalArgumentException("Invalid Code");

            if(!(passwordChangeDTO.getNewPass().equals(passwordChangeDTO.getRepeatPassword()))) throw new IllegalArgumentException("Password does not match with repeat password");

            if(!(isValidPassword(passwordChangeDTO.getNewPass()))) throw new IllegalArgumentException("Pasword must contain atleast 8 characters including uppercase, lowercase, number and a special character");

            AD ad = passwordChange.getAd();
            ad.setPassword(encoder.encode(passwordChangeDTO.getNewPass()));
            adRepository.save(ad);

            passwordChangeRepository.delete(passwordChange);

            return "Password Successfully changed";
        }

}
