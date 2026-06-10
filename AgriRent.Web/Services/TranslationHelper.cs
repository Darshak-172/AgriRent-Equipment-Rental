using System.Collections.Generic;

namespace AgriRent.Web.Services
{
    public static class TranslationHelper
    {
        private static readonly Dictionary<string, Dictionary<string, string>> Translations = new()
        {
            ["en"] = new Dictionary<string, string>
            {
                // Navigation
                ["Home"] = "Home",
                ["RentEquipment"] = "Rent Equipment",
                ["BuyProducts"] = "Buy Products",
                ["Contact"] = "Contact",
                ["Login"] = "Login",
                ["Logout"] = "Logout",
                
                // Profile
                ["ViewProfile"] = "View Profile",
                ["EditProfile"] = "Edit Profile",
                ["ChangePassword"] = "Change Password",
                ["MyProfile"] = "My Profile",
                ["PersonalDetails"] = "Personal Details",
                ["Security"] = "Security",
                
                // Form Fields
                ["FullName"] = "Full Name",
                ["Username"] = "Username",
                ["MobileNumber"] = "Mobile Number",
                ["Email"] = "Email",
                ["Password"] = "Password",
                ["Role"] = "Role",
                ["OldPassword"] = "Old Password",
                ["NewPassword"] = "New Password",
                ["ConfirmPassword"] = "Confirm Password",
                
                // Buttons
                ["Save"] = "Save",
                ["Cancel"] = "Cancel",
                ["UpdatePassword"] = "Update Password",
                ["Submit"] = "Submit",
                ["SendOTP"] = "Send OTP",
                ["CompleteRegistration"] = "Complete Registration",
                ["ViewAll"] = "View All",
                ["RentNow"] = "Rent Now",
                ["JoinNow"] = "Join Now",
                
                // Auth Pages
                ["WelcomeBack"] = "Welcome Back",
                ["WelcomeSubtext"] = "Ready to manage your farm or rent equipment?",
                ["JoinAgriRent"] = "Join AgriRent",
                ["JoinSubtext"] = "Start renting equipment and selling products today!",
                ["DontHaveAccount"] = "Don't have an account?",
                ["AlreadyHaveAccount"] = "Already have an account?",
                ["SignUp"] = "Sign Up",
                ["SignIn"] = "Sign In",
                ["ForgotPassword"] = "Forgot Password?",
                ["ForgotPasswordTitle"] = "Forgot Password",
                ["ResetPassword"] = "Reset Password",
                ["EnterMobileHint"] = "Enter 10-digit mobile number",
                ["EnterNameHint"] = "Enter characters only (A-Z, a-z)",
                ["EnterMobileOTP"] = "Enter your mobile number to receive an OTP",
                ["EnterOTPNewPassword"] = "Enter OTP and your new password",
                ["RememberPassword"] = "Remember your password?",
                ["OTPCode"] = "OTP Code",
                ["ResetPasswordButton"] = "Reset Password",
                
                // Homepage
                ["SearchPlaceholder"] = "Search tractor, harvester...",
                ["Location"] = "Location",
                ["Category"] = "Category",
                ["EquipmentCategories"] = "Equipment Categories",
                ["FeaturedEquipment"] = "Featured Equipment",
                ["FreshFromFarm"] = "Fresh From Farm",
                ["HowItWorks"] = "How AgriRent Works",
                ["WhyChooseUs"] = "Why Choose AgriRent?",
                ["WhatFarmersSay"] = "What Farmers Say",
                ["MoreEquipment"] = "More Equipment",
                ["MoreProducts"] = "More Products",
                
                // Hero Carousel
                ["HeroTitle1"] = "Rent Farm Equipment Easily",
                ["HeroSubtitle1"] = "Find tractors, harvesters, and more near you.",
                ["HeroTitle2"] = "Buy Fresh Farm Produce",
                ["HeroSubtitle2"] = "Directly from farmers to your doorstep.",
                ["HeroTitle3"] = "Empowering Farmers",
                ["HeroSubtitle3"] = "Join the AgriRent community today.",
                
                // Section Subtitles
                ["FindWhatYouNeed"] = "Find exactly what you need for your farm",
                ["AdminWillAddCategories"] = "Admin will add categories soon.",
                ["TopRatedRentals"] = "Top rated rentals available near you",
                ["NoEquipmentAvailable"] = "No equipment available at the moment.",
                ["BuyFromLocalFarmers"] = "Buy directly from local farmers",
                ["NoProductsAvailable"] = "No products available at the moment.",
                ["SimpleSteps"] = "Simple steps to get started",
                ["RealStories"] = "Real stories from our community",
                
                // How It Works Steps
                ["StepSignUpTitle"] = "Sign Up",
                ["StepSignUpDesc"] = "Create your account as a farmer or equipment owner easily.",
                ["StepSearchTitle"] = "Search",
                ["StepSearchDesc"] = "Find the equipment you need or list your farm products.",
                ["StepRentBuyTitle"] = "Rent or Buy",
                ["StepRentBuyDesc"] = "Book equipment for your dates or buy produce instantly.",
                ["StepDeliveryTitle"] = "Delivery",
                ["StepDeliveryDesc"] = "Get hassle-free delivery or pickup at your location.",
                
                // Why Choose Us Features
                ["AffordablePricing"] = "Affordable Pricing",
                ["AffordablePricingDesc"] = "Transparent pricing per hour/day. No hidden charges.",
                ["VerifiedPartners"] = "Verified Partners",
                ["VerifiedPartnersDesc"] = "All equipment owners and sellers are verified for safety.",
                ["Support247"] = "24/7 Support",
                ["Support247Desc"] = "Dedicated support team to help you with booking and issues.",
                
                // Testimonials
                ["Testimonial1"] = "AgriRent made it so easy to find a tractor during peak season. Saved me a lot of time and money!",
                ["Testimonial1Name"] = "Rahul Singh",
                ["Testimonial1Location"] = "Farmer, Nashik",
                ["Testimonial2"] = "I rented out my harvester when it was idle. It's a great source of extra income for me.",
                ["Testimonial2Name"] = "Vikram Patil",
                ["Testimonial2Location"] = "Owner, Pune",
                ["Testimonial3"] = "Buying fresh vegetables directly from farmers is amazing. The quality is top-notch!",
                ["Testimonial3Name"] = "Anita Deshmukh",
                ["Testimonial3Location"] = "Buyer, Mumbai",
                
                // Misc
                ["FreshProduce"] = "Fresh Produce",
                ["LocationLabel"] = "Location:",
                ["Loading"] = "Loading",
                ["LoadingAgriRent"] = "Loading AgriRent...",
                ["PleaseWait"] = "Please wait",
                
                // Footer
                ["FooterTagline"] = "Empowering farmers with affordable equipment rentals and a direct marketplace for fresh produce.",
                ["QuickLinks"] = "Quick Links",
                ["Support"] = "Support",
                ["AboutUs"] = "About Us",
                ["HelpCenter"] = "Help Center",
                ["TermsOfService"] = "Terms of Service",
                ["PrivacyPolicy"] = "Privacy Policy",
                ["ContactUs"] = "Contact Us",
                ["AllRightsReserved"] = "All rights reserved."
            },
            ["gu"] = new Dictionary<string, string>
            {
                // Navigation
                ["Home"] = "હોમ",
                ["RentEquipment"] = "સાધનો ભાડે લો",
                ["BuyProducts"] = "ઉત્પાદનો ખરીદો",
                ["Contact"] = "સંપર્ક",
                ["Login"] = "લૉગિન",
                ["Logout"] = "લૉગઆઉટ",
                
                // Profile
                ["ViewProfile"] = "પ્રોફાઇલ જુઓ",
                ["EditProfile"] = "પ્રોફાઇલ સંપાદિત કરો",
                ["ChangePassword"] = "પાસવર્ડ બદલો",
                ["MyProfile"] = "મારી પ્રોફાઇલ",
                ["PersonalDetails"] = "વ્યક્તિગત વિગતો",
                ["Security"] = "સુરક્ષા",
                
                // Form Fields
                ["FullName"] = "પૂરું નામ",
                ["Username"] = "વપરાશકર્તા નામ",
                ["MobileNumber"] = "મોબાઇલ નંબર",
                ["Email"] = "ઈમેલ",
                ["Password"] = "પાસવર્ડ",
                ["Role"] = "ભૂમિકા",
                ["OldPassword"] = "જૂનો પાસવર્ડ",
                ["NewPassword"] = "નવો પાસવર્ડ",
                ["ConfirmPassword"] = "પાસવર્ડની પુષ્ટિ કરો",
                
                // Buttons
                ["Save"] = "સાચવો",
                ["Cancel"] = "રદ કરો",
                ["UpdatePassword"] = "પાસવર્ડ અપડેટ કરો",
                ["Submit"] = "સબમિટ કરો",
                ["SendOTP"] = "OTP મોકલો",
                ["CompleteRegistration"] = "નોંધણી પૂર્ણ કરો",
                ["ViewAll"] = "બધા જુઓ",
                ["RentNow"] = "હવે ભાડે લો",
                ["JoinNow"] = "હવે જોડાઓ",
                
                // Auth Pages
                ["WelcomeBack"] = "ફરી સ્વાગત છે",
                ["WelcomeSubtext"] = "તમારા ખેતર અથવા ભાડા સાધનોનું સંચાલન કરવા તૈયાર છો?",
                ["JoinAgriRent"] = "એગ્રીરેન્ટમાં જોડાઓ",
                ["JoinSubtext"] = "આજે જ સાધનો ભાડે લેવાનું અને ઉત્પાદનો વેચવાનું શરૂ કરો!",
                ["DontHaveAccount"] = "શું તમારી પાસે એકાઉન્ટ નથી?",
                ["AlreadyHaveAccount"] = "શું તમારી પાસે પહેલેથી એકાઉન્ટ છે?",
                ["SignUp"] = "સાઇન અપ",
                ["SignIn"] = "સાઇન ઇન",
                ["ForgotPassword"] = "પાસવર્ડ ભૂલી ગયા?",
                ["ForgotPasswordTitle"] = "પાસવર્ડ ભૂલી ગયા",
                ["ResetPassword"] = "પાસવર્ડ રીસેટ કરો",
                ["EnterMobileHint"] = "10 આંકડાનો મોબાઇલ નંબર દાખલ કરો",
                ["EnterNameHint"] = "માત્ર અક્ષરો દાખલ કરો (A-Z, a-z)",
                ["EnterMobileOTP"] = "OTP મેળવવા માટે તમારો મોબાઇલ નંબર દાખલ કરો",
                ["EnterOTPNewPassword"] = "OTP અને તમારો નવો પાસવર્ડ દાખલ કરો",
                ["RememberPassword"] = "તમારો પાસવર્ડ યાદ છે?",
                ["OTPCode"] = "OTP કોડ",
                ["ResetPasswordButton"] = "પાસવર્ડ રીસેટ કરો",
                
                // Homepage
                ["SearchPlaceholder"] = "ટ્રેક્ટર, હાર્વેસ્ટર શોધો...",
                ["Location"] = "સ્થાન",
                ["Category"] = "શ્રેણી",
                ["EquipmentCategories"] = "સાધન શ્રેણીઓ",
                ["FeaturedEquipment"] = "વિશિષ્ટ સાધનો",
                ["FreshFromFarm"] = "ખેતરમાંથી તાજું",
                ["HowItWorks"] = "એગ્રીરેન્ટ કેવી રીતે કાર્ય કરે છે",
                ["WhyChooseUs"] = "એગ્રીરેન્ટ કેમ પસંદ કરો?",
                ["WhatFarmersSay"] = "ખેડૂતો શું કહે છે",
                ["MoreEquipment"] = "વધુ સાધનો",
                ["MoreProducts"] = "વધુ ઉત્પાદનો",
                
                // Hero Carousel
                ["HeroTitle1"] = "ખેતીના સાધનો સરળતાથી ભાડે લો",
                ["HeroSubtitle1"] = "તમારી નજીક ટ્રેક્ટર, હાર્વેસ્ટર અને વધુ શોધો.",
                ["HeroTitle2"] = "તાજા ખેત ઉત્પાદનો ખરીદો",
                ["HeroSubtitle2"] = "સીધા ખેડૂતો પાસેથી તમારા ઘર સુધી.",
                ["HeroTitle3"] = "ખેડૂતોને સશક્ત બનાવવું",
                ["HeroSubtitle3"] = "આજે જ એગ્રીરેન્ટ સમુદાયમાં જોડાઓ.",
                
                // Section Subtitles
                ["FindWhatYouNeed"] = "તમારા ખેતર માટે જરૂરી બધું શોધો",
                ["AdminWillAddCategories"] = "એડમિન ટૂંક સમયમાં શ્રેણીઓ ઉમેરશે.",
                ["TopRatedRentals"] = "તમારી નજીક ટોચના રેટેડ ભાડા ઉપલબ્ધ છે",
                ["NoEquipmentAvailable"] = "આ ક્ષણે કોઈ સાધન ઉપલબ્ધ નથી.",
                ["BuyFromLocalFarmers"] = "સ્થાનિક ખેડૂતો પાસેથી સીધા ખરીદો",
                ["NoProductsAvailable"] = "આ ક્ષણે કોઈ ઉત્પાદન ઉપલબ્ધ નથી.",
                ["SimpleSteps"] = "શરૂ કરવા માટેના સરળ પગલાં",
                ["RealStories"] = "અમારા સમુદાયની વાસ્તવિક વાર્તાઓ",
                
                // How It Works Steps
                ["StepSignUpTitle"] = "સાઇન અપ",
                ["StepSignUpDesc"] = "ખેડૂત અથવા સાધન માલિક તરીકે સરળતાથી તમારું એકાઉન્ટ બનાવો.",
                ["StepSearchTitle"] = "શોધો",
                ["StepSearchDesc"] = "તમને જરૂરી સાધનો શોધો અથવા તમારા ખેત ઉત્પાદનોની યાદી બનાવો.",
                ["StepRentBuyTitle"] = "ભાડે લો અથવા ખરીદો",
                ["StepRentBuyDesc"] = "તમારી તારીખો માટે સાધનો બુક કરો અથવા તરત ઉત્પાદન ખરીદો.",
                ["StepDeliveryTitle"] = "ડિલિવરી",
                ["StepDeliveryDesc"] = "તમારા સ્થાન પર હેસલ-ફ્રી ડિલિવરી અથવા પિકઅપ મેળવો.",
                
                // Why Choose Us Features
                ["AffordablePricing"] = "સસ્તી કિંમત",
                ["AffordablePricingDesc"] = "કલાક/દિવસ દીઠ પારદર્શક કિંમત. કોઈ છુપાયેલા શુલ્ક નથી.",
                ["VerifiedPartners"] = "ચકાસાયેલ ભાગીદારો",
                ["VerifiedPartnersDesc"] = "બધા સાધન માલિકો અને વેચાણકર્તાઓ સુરક્ષા માટે ચકાસાયેલા છે.",
                ["Support247"] = "24/7 સહાય",
                ["Support247Desc"] = "બુકિંગ અને સમસ્યાઓમાં મદદ માટે સમર્પિત સપોર્ટ ટીમ.",
                
                // Testimonials
                ["Testimonial1"] = "એગ્રીરેન્ટે પીક સીઝનમાં ટ્રેક્ટર શોધવાનું ખૂબ સરળ બનાવ્યું. મારો ઘણો સમય અને પૈસા બચાવ્યા!",
                ["Testimonial1Name"] = "રાહુલ સિંહ",
                ["Testimonial1Location"] = "ખેડૂત, નાસિક",
                ["Testimonial2"] = "જ્યારે મારું હાર્વેસ્ટર નિષ્ક્રિય હતું ત્યારે મેં તેને ભાડે આપ્યું. તે મારા માટે વધારાની આવકનો સરસ સ્રોત છે.",
                ["Testimonial2Name"] = "વિક્રમ પાટીલ",
                ["Testimonial2Location"] = "માલિક, પુણે",
                ["Testimonial3"] = "ખેડૂતો પાસેથી સીધા તાજા શાકભાજી ખરીદવા અદ્ભુત છે. ગુણવત્તા ટોચની છે!",
                ["Testimonial3Name"] = "અનિતા દેશમુખ",
                ["Testimonial3Location"] = "ખરીદનાર, મુંબઈ",
                
                // Misc
                ["FreshProduce"] = "તાજા ઉત્પાદનો",
                ["LocationLabel"] = "સ્થાન:",
                ["Loading"] = "લોડ થઈ રહ્યું છે",
                ["LoadingAgriRent"] = "એગ્રીરેન્ટ લોડ થઈ રહ્યું છે...",
                ["PleaseWait"] = "કૃપા કરીને રાહ જુઓ",
                
                // Footer
                ["FooterTagline"] = "ખેડૂતોને સસ્તા સાધનોના ભાડા અને તાજા ઉત્પાદનો માટે સીધા બજાર સાથે સશક્ત બનાવવું.",
                ["QuickLinks"] = "ઝડપી લિંક્સ",
                ["Support"] = "સહાય",
                ["AboutUs"] = "અમારા વિશે",
                ["HelpCenter"] = "સહાય કેન્દ્ર",
                ["TermsOfService"] = "સેવાની શરતો",
                ["PrivacyPolicy"] = "ગોપનીયતા નીતિ",
                ["ContactUs"] = "અમારો સંપર્ક કરો",
                ["AllRightsReserved"] = "સર્વ અધિકારો અનામત."
            },
            ["hi"] = new Dictionary<string, string>
            {
                // Navigation
                ["Home"] = "होम",
                ["RentEquipment"] = "उपकरण किराए पर लें",
                ["BuyProducts"] = "उत्पाद खरीदें",
                ["Contact"] = "संपर्क",
                ["Login"] = "लॉगिन",
                ["Logout"] = "लॉगआउट",
                
                // Profile
                ["ViewProfile"] = "प्रोफ़ाइल देखें",
                ["EditProfile"] = "प्रोफ़ाइल संपादित करें",
                ["ChangePassword"] = "पासवर्ड बदलें",
                ["MyProfile"] = "मेरी प्रोफ़ाइल",
                ["PersonalDetails"] = "व्यक्तिगत विवरण",
                ["Security"] = "सुरक्षा",
                
                // Form Fields
                ["FullName"] = "पूरा नाम",
                ["Username"] = "उपयोगकर्ता नाम",
                ["MobileNumber"] = "मोबाइल नंबर",
                ["Email"] = "ईमेल",
                ["Password"] = "पासवर्ड",
                ["Role"] = "भूमिका",
                ["OldPassword"] = "पुराना पासवर्ड",
                ["NewPassword"] = "नया पासवर्ड",
                ["ConfirmPassword"] = "पासवर्ड की पुष्टि करें",
                
                // Buttons
                ["Save"] = "सहेजें",
                ["Cancel"] = "रद्द करें",
                ["UpdatePassword"] = "पासवर्ड अपडेट करें",
                ["Submit"] = "सबमिट करें",
                ["SendOTP"] = "OTP भेजें",
                ["CompleteRegistration"] = "पंजीकरण पूरा करें",
                ["ViewAll"] = "सभी देखें",
                ["RentNow"] = "अभी किराए पर लें",
                ["JoinNow"] = "अभी शामिल हों",
                
                // Auth Pages
                ["WelcomeBack"] = "वापसी पर स्वागत है",
                ["WelcomeSubtext"] = "अपने फार्म या किराए के उपकरणों का प्रबंधन करने के लिए तैयार हैं?",
                ["JoinAgriRent"] = "एग्रीरेंट में शामिल हों",
                ["JoinSubtext"] = "आज ही उपकरण किराए पर लेना और उत्पाद बेचना शुरू करें!",
                ["DontHaveAccount"] = "क्या आपके पास खाता नहीं है?",
                ["AlreadyHaveAccount"] = "क्या आपके पास पहले से खाता है?",
                ["SignUp"] = "साइन अप",
                ["SignIn"] = "साइन इन",
                ["ForgotPassword"] = "पासवर्ड भूल गए?",
                ["ForgotPasswordTitle"] = "पासवर्ड भूल गए",
                ["ResetPassword"] = "पासवर्ड रीसेट करें",
                ["EnterMobileHint"] = "10 अंकों का मोबाइल नंबर दर्ज करें",
                ["EnterNameHint"] = "केवल अक्षर दर्ज करें (A-Z, a-z)",
                ["EnterMobileOTP"] = "OTP प्राप्त करने के लिए अपना मोबाइल नंबर दर्ज करें",
                ["EnterOTPNewPassword"] = "OTP और अपना नया पासवर्ड दर्ज करें",
                ["RememberPassword"] = "क्या आपको पासवर्ड याद है?",
                ["OTPCode"] = "OTP कोड",
                ["ResetPasswordButton"] = "पासवर्ड रीसेट करें",
                
                // Homepage
                ["SearchPlaceholder"] = "ट्रैक्टर, हार्वेस्टर खोजें...",
                ["Location"] = "स्थान",
                ["Category"] = "श्रेणी",
                ["EquipmentCategories"] = "उपकरण श्रेणियां",
                ["FeaturedEquipment"] = "विशेष उपकरण",
                ["FreshFromFarm"] = "खेत से ताजा",
                ["HowItWorks"] = "एग्रीरेंट कैसे काम करता है",
                ["WhyChooseUs"] = "एग्रीरेंट क्यों चुनें?",
                ["WhatFarmersSay"] = "किसान क्या कहते हैं",
                ["MoreEquipment"] = "अधिक उपकरण",
                ["MoreProducts"] = "अधिक उत्पाद",
                
                // Hero Carousel
                ["HeroTitle1"] = "खेती के उपकरण आसानी से किराए पर लें",
                ["HeroSubtitle1"] = "अपने पास ट्रैक्टर, हार्वेस्टर और अधिक खोजें।",
                ["HeroTitle2"] = "ताजा खेत उपज खरीदें",
                ["HeroSubtitle2"] = "सीधे किसानों से आपके घर तक।",
                ["HeroTitle3"] = "किसानों को सशक्त बनाना",
                ["HeroSubtitle3"] = "आज ही एग्रीरेंट समुदाय में शामिल हों।",
                
                // Section Subtitles
                ["FindWhatYouNeed"] = "अपने खेत के लिए जो चाहिए वो खोजें",
                ["AdminWillAddCategories"] = "प्रशासक जल्द ही श्रेणियां जोड़ेंगे।",
                ["TopRatedRentals"] = "आपके पास शीर्ष रेटेड किराये उपलब्ध हैं",
                ["NoEquipmentAvailable"] = "इस समय कोई उपकरण उपलब्ध नहीं है।",
                ["BuyFromLocalFarmers"] = "स्थानीय किसानों से सीधे खरीदें",
                ["NoProductsAvailable"] = "इस समय कोई उत्पाद उपलब्ध नहीं है।",
                ["SimpleSteps"] = "शुरू करने के लिए सरल कदम",
                ["RealStories"] = "हमारे समुदाय की वास्तविक कहानियां",
                
                // How It Works Steps
                ["StepSignUpTitle"] = "साइन अप",
                ["StepSignUpDesc"] = "किसान या उपकरण मालिक के रूप में आसानी से अपना खाता बनाएं।",
                ["StepSearchTitle"] = "खोजें",
                ["StepSearchDesc"] = "अपने लिए जरूरी उपकरण खोजें या अपने खेत उत्पाद सूचीबद्ध करें।",
                ["StepRentBuyTitle"] = "किराए पर लें या खरीदें",
                ["StepRentBuyDesc"] = "अपनी तारीखों के लिए उपकरण बुक करें या तुरंत उपज खरीदें।",
                ["StepDeliveryTitle"] = "डिलीवरी",
                ["StepDeliveryDesc"] = "अपने स्थान पर परेशानी मुक्त डिलीवरी या पिकअप प्राप्त करें।",
                
                // Why Choose Us Features
                ["AffordablePricing"] = "किफायती मूल्य",
                ["AffordablePricingDesc"] = "प्रति घंटा/दिन पारदर्शी मूल्य। कोई छिपा शुल्क नहीं।",
                ["VerifiedPartners"] = "सत्यापित भागीदार",
                ["VerifiedPartnersDesc"] = "सभी उपकरण मालिक और विक्रेता सुरक्षा के लिए सत्यापित हैं।",
                ["Support247"] = "24/7 सहायता",
                ["Support247Desc"] = "बुकिंग और समस्याओं में मदद के लिए समर्पित सहायता टीम।",
                
                // Testimonials
                ["Testimonial1"] = "एग्रीरेंट ने पीक सीजन में ट्रैक्टर खोजना बहुत आसान बना दिया। मेरा बहुत समय और पैसा बचा!",
                ["Testimonial1Name"] = "राहुल सिंह",
                ["Testimonial1Location"] = "किसान, नासिक",
                ["Testimonial2"] = "जब मेरा हार्वेस्टर निष्क्रिय था तो मैंने इसे किराए पर दिया। यह मेरे लिए अतिरिक्त आय का एक बढ़िया स्रोत है।",
                ["Testimonial2Name"] = "विक्रम पाटिल",
                ["Testimonial2Location"] = "मालिक, पुणे",
                ["Testimonial3"] = "किसानों से सीधे ताजी सब्जियां खरीदना अद्भुत है। गुणवत्ता शीर्ष स्तर की है!",
                ["Testimonial3Name"] = "अनिता देशमुख",
                ["Testimonial3Location"] = "खरीदार, मुंबई",
                
                // Misc
                ["FreshProduce"] = "ताजा उपज",
                ["LocationLabel"] = "स्थान:",
                ["Loading"] = "लोड हो रहा है",
                ["LoadingAgriRent"] = "एग्रीरेंट लोड हो रहा है...",
                ["PleaseWait"] = "कृपया प्रतीक्षा करें",
                
                // Footer
                ["FooterTagline"] = "किसानों को सस्ते उपकरण किराए और ताज़ा उपज के लिए सीधे बाज़ार के साथ सशक्त बनाना।",
                ["QuickLinks"] = "त्वरित लिंक",
                ["Support"] = "सहायता",
                ["AboutUs"] = "हमारे बारे में",
                ["HelpCenter"] = "सहायता केंद्र",
                ["TermsOfService"] = "सेवा की शर्तें",
                ["PrivacyPolicy"] = "गोपनीयता नीति",
                ["ContactUs"] = "हमसे संपर्क करें",
                ["AllRightsReserved"] = "सर्वाधिकार सुरक्षित।"
            }
        };

        public static string Translate(string key, string language)
        {
            if (string.IsNullOrEmpty(language) || !Translations.ContainsKey(language))
            {
                language = "en";
            }

            if (Translations[language].ContainsKey(key))
            {
                return Translations[language][key];
            }

            // Fallback to English if key not found
            return Translations["en"].ContainsKey(key) ? Translations["en"][key] : key;
        }
    }
}
