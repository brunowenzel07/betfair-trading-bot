package com.tradingbot.ui;

import com.betfair.aping.exceptions.APINGException;
import com.betfair.aping.util.SessionTokenDiscoverer;
import com.google.common.collect.Lists;
import com.tradingbot.*;
import com.tradingbot.entities.*;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.data.Property;
import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.vaadin.highcharts.HighChart;

import javax.servlet.http.Cookie;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

@Theme("valo")
@Push(PushMode.MANUAL)
public class Page extends UI {

    private static final Logger LOG = Logger.getLogger(Page.class);
    private static final Map<String, TradingEngine> userEngineMap = new HashMap<>();
    private static final String TOKEN_DISCOVERER = "tokenDiscoverer";
    private static final String EVENTS_LOG = "eventsLog";
    private static final String USER = "user";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private ResourceBundle bundle = ResourceBundle.getBundle(Captions.class.getName(), resolveLocale());

    private Locale resolveLocale() {
        for (Cookie cookie : VaadinService.getCurrentRequest().getCookies()) {
            if(cookie.getName().equals("lang")) {
                return new Locale(cookie.getValue());
            }
        }
        return VaadinSession.getCurrent().getLocale();
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        if (getSession().getAttribute(USER) != null) {
            try {
                setContent(buildDashboard());
            } catch (Exception | APINGException e) {
                LOG.error(String.format("Failed to build dashboard. username='%s'", getCurrentUsername()), e);
                Notification.show(e.getMessage()+". "+bundle.getString(Captions.errorPostfixMsg), Notification.Type.ERROR_MESSAGE);
            }
        } else {
            setContent(buildLoginForm());
        }
        UI.getCurrent().getPage().setTitle("Trading engine");
    }

    private Component buildLoginForm() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.addStyleName("login-background");
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        LoginForm loginForm = new LoginForm(){
            @Override
            protected Component createContent(TextField userName, PasswordField password, Button loginButton) {
                return createLoginFormContent(userName, password, loginButton);
            }
        };
        loginForm.addLoginListener(createLoginListener());
        layout.addComponent(loginForm);
        return layout;
    }

    private LoginForm.LoginListener createLoginListener() {
        return (LoginForm.LoginListener) loginEvent -> {
            String username = loginEvent.getLoginParameter(USERNAME).trim();
            String password = loginEvent.getLoginParameter(PASSWORD);
            if(StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                Notification.show(bundle.getString(Captions.blankUsernameMsg), Notification.Type.WARNING_MESSAGE);
                return;
            }
            try {
                SessionTokenDiscoverer tokenDiscoverer = BetfairUtils.createTokenDiscoverer(username, password);
                VaadinSession session = VaadinSession.getCurrent();
                session.setAttribute(TOKEN_DISCOVERER, tokenDiscoverer);
                session.setAttribute(EVENTS_LOG, createLogArea());
                userLoggedIn(tokenDiscoverer, username);
                LOG.debug(String.format("User '%s' successfully logged in.", username));
                setContent(buildDashboard());
            } catch (APINGException | Exception e) {
                String errorMsg = e.getMessage();
                if (StringUtils.contains(errorMsg, "INVALID_USERNAME_OR_PASSWORD")) {
                    Notification.show(bundle.getString(Captions.invalidUsernameMsg), bundle.getString(Captions.subscriptionMsg),
                            Notification.Type.WARNING_MESSAGE);
                } else if (StringUtils.contains(errorMsg, "INVALID_APP_KEY")) {
                    Notification.show(bundle.getString(Captions.subscriptionMsg), Notification.Type.WARNING_MESSAGE);
                } else {
                    LOG.error("An error occurred during login process:", e);
                    Notification.show(bundle.getString(Captions.errorPrefixMsg)+": "+errorMsg+". "
                            +bundle.getString(Captions.errorPostfixMsg), Notification.Type.ERROR_MESSAGE);
                }
            }
        };
    }

    private Component createLoginFormContent(TextField userNameField, PasswordField passwordField, Button loginButton) {
        userNameField.setCaption(bundle.getString(Captions.username));
        passwordField.setCaption(bundle.getString(Captions.password));
        loginButton.setCaption(bundle.getString(Captions.login));
        userNameField.setInputPrompt(bundle.getString(Captions.usernamePrompt));
        userNameField.setRequired(true);
        passwordField.setRequired(true);
        userNameField.setWidth(12f, Unit.EM);
        passwordField.setWidth(12f, Unit.EM);
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        Label label = new Label("");
        label.setStyleName("h1");
        layout.addComponent(label);
        layout.addComponent(createSupportLink());
        layout.addComponent(userNameField);
        layout.addComponent(passwordField);
        layout.addComponent(loginButton);
        return layout;
    }

    private Link createSupportLink() {
        Link link = new Link(bundle.getString(Captions.support), new ExternalResource("http://vk.com/write299695949"));
        link.setTargetName("_blank");
        return link;
    }

    private void userLoggedIn(SessionTokenDiscoverer tokenDiscoverer, String username) throws Exception, APINGException {
        AccountDetails details = BetfairUtils.getAccountDetails(tokenDiscoverer);
        WebBrowser webBrowser = com.vaadin.server.Page.getCurrent().getWebBrowser();
        String ipAddress = webBrowser.getAddress();
        double balance = details.getBalance();
        User userObject = DBHelper.getUser(username);
        if (userObject != null) {
            DBHelper.updateUser(username, user -> {
                user.setLastLogin(new Date());
                user.setIpAddress(ipAddress);
                user.setBalance(balance);
            });
        } else {
            userObject = new User(username, details.getFullName(), balance, details.getCurrency(), ipAddress, getDefaultTradingConfig());
            DBHelper.addUser(userObject);
        }
        getSession().setAttribute(USER, userObject);
    }

    private Component buildDashboard() throws Exception, APINGException {
        VaadinSession session = VaadinSession.getCurrent();
        SessionTokenDiscoverer tokenDiscoverer = (SessionTokenDiscoverer) session.getAttribute(TOKEN_DISCOVERER);
        User user = (User) session.getAttribute(USER);
        TextArea eventsLog = (TextArea) session.getAttribute(EVENTS_LOG);
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        addStyleName("dashboard-background");
        AccountDetails accountDetails = BetfairUtils.getAccountDetails(tokenDiscoverer);
        TradingEngine tradingEngine = userEngineMap.computeIfAbsent(user.getUserName(), k -> new TradingEngine(user, tokenDiscoverer));
        updateTradingEngine(tokenDiscoverer, user, tradingEngine);
        layout.addComponent(createHeader(user, accountDetails, tradingEngine));
        HighChart chart = createChart();
        layout.addComponent(createPeriodSelect(chart));
        layout.addComponent(chart);
        layout.addComponent(eventsLog);
        return layout;
    }

    private HighChart createChart() {
        HighChart chart = new HighChart();
        chart.setWidth("85%");
        return chart;
    }

    private Component createPeriodSelect(HighChart chart) {
        List<String> options = Lists.newArrayList(
                bundle.getString(Captions.today), bundle.getString(Captions.last7days), bundle.getString(Captions.last30days));
        HorizontalLayout layout = new HorizontalLayout();
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        layout.setSpacing(true);
        Label profitLabel = new Label();
        profitLabel.setContentMode(ContentMode.HTML);
        ComboBox comboBox = new ComboBox();
        comboBox.addItems(options);
        comboBox.setTextInputAllowed(false);
        comboBox.setNullSelectionAllowed(false);
        comboBox.addValueChangeListener((Property.ValueChangeListener) event ->
                populateChart(chart, profitLabel, event.getProperty().getValue().toString()));
        comboBox.select(bundle.getString(Captions.today));
        layout.addComponents(comboBox, profitLabel);
        String userName = getCurrentUsername();
        User user = DBHelper.getUser(userName);
        if(user == null) {
            LOG.warn(String.format("User not found. userName='%s'", userName));
            return layout;
        }
        boolean demoPeriodActivated = !user.getExpiryDate().equals(user.getCreated());
        boolean subscriptionExpired = System.currentTimeMillis() > user.getExpiryDate().getTime();
        if(user.isDiscountable() && demoPeriodActivated && subscriptionExpired) {
            layout.addComponent(createDiscountButton(user));
        }
        return layout;
    }

    private Component createDiscountButton(User user) {
        return new Button(bundle.getString(Captions.getDiscount), (Button.ClickListener) event -> {
            Discount discount = user.getDiscount();
            if(discount == null) {
                discount = createDiscount(user);
                LOG.debug(String.format("Discount created for user '%s'", user.getUserName()));
            }
            Window subWindow = new Window(String.format(bundle.getString(Captions.yourDiscount), discount.getPercent()));
            subWindow.setWidth(30f, Unit.EM);
            FormLayout content = new FormLayout();
            content.setMargin(true);
            subWindow.setContent(content);
            content.addComponent(new Label(String.format(bundle.getString(Captions.discountCode)+": <b>%s</b>", discount.getCode()), ContentMode.HTML));
            content.addComponent(new Label(bundle.getString(Captions.discountExpires)+": "+createExpiryString(discount.getExpiryDate()), ContentMode.HTML));
            subWindow.center();
            subWindow.setModal(true);
            subWindow.setCaptionAsHtml(true);
            addWindow(subWindow);
        });
    }

    private Discount createDiscount(User user) {
        long now = System.currentTimeMillis();
        Discount discount = new Discount(now+"", 20d, new Date(now + TimeUnit.DAYS.toMillis(1)));
        user.setDiscount(discount);
        DBHelper.updateUser(user);
        return discount;
    }

    private void populateChart(HighChart chart, Label profitLabel, String period) {
        List<BetReport> betReports = getBetReports(getStartDate(period), new Date());
        List<String> categories = new ArrayList<>();
        List<String> data = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d-MMM H:mm", Locale.ENGLISH);
        double profitCounter = 0;
        for (BetReport betReport : betReports) {
            double profitValue = betReport.getProfit();
            String profitString = profitValue < 0 ? ""+profitValue : "+"+profitValue;
            String courtName = betReport.getMarketName().split(":")[0].split(" ")[0];
            String marketName = betReport.getMarketName().split(":")[1];
            categories.add(String.format("'<b>%s</b><br/>%s %s<br/>%s'", profitString, courtName, dateFormat.format(betReport.getStartTime()), marketName));
            profitCounter += profitValue;
            profitCounter = CalcUtils.round(profitCounter, 2);
            data.add(profitCounter+"");
        }
        User user = getCurrentUser();
        String currency = user != null ? user.getCurrency() : StringUtils.EMPTY;
        profitLabel.setValue(String.format(bundle.getString(Captions.profit)+": <b>%s %s</b>", profitCounter, currency));
        chart.setHcjs(buildChartScript(categories, data));
    }

    private Date getStartDate(String periodString) {
        Calendar calendar = Calendar.getInstance();
        if(bundle.getString(Captions.today).equals(periodString)) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        if(bundle.getString(Captions.last7days).equals(periodString)) {
            calendar.add(Calendar.DATE, -7);
        }
        if(bundle.getString(Captions.last30days).equals(periodString)) {
            calendar.add(Calendar.DATE, -30);
        }
        return calendar.getTime();
    }

    private List<BetReport> getBetReports(Date startDate, Date endDate) {
        List<BetReport> result = new ArrayList<>();
        User user = DBHelper.getUser(getCurrentUsername());
        List<BetReport> betReports = user.getBetReports();
        for (BetReport betReport : betReports) {
            Date eventStartTime = betReport.getStartTime();
            if(eventStartTime.after(startDate) && eventStartTime.before(endDate)) {
                result.add(betReport);
            }
        }
        result.sort((o1, o2) -> (int) (o1.getStartTime().getTime() - o2.getStartTime().getTime()));
        return result;
    }


    private String buildChartScript(List<String> categories, List<String> data) {
        return String.format("var options = {\n" +
                "    title: {text: '%s'},\n" +
                "    tooltip: {crosshairs: [true, true] },\n" +
                "    yAxis: {title: {text: 'Profit'}},\n" +
                "    xAxis: {title: {text: 'Markets'},\n" +
                "        labels: {enabled: false},\n" +
                "        categories: [%s]},\n" +
                "    series: [{name: '%s',\n" +
                "        data: [%s]\n" +
                "    }]\n" +
                "};",
                bundle.getString(Captions.tradingHistory),
                StringUtils.join(categories, ","),
                bundle.getString(Captions.profit),
                StringUtils.join(data, ","));
    }

    private HorizontalLayout createHeader(User user, AccountDetails accountDetails, TradingEngine tradingEngine) {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        header.addComponent(new Label(bundle.getString(Captions.welcome)+": " + wrapBold(accountDetails.getFullName()), ContentMode.HTML));
        header.addComponent(new Label(bundle.getString(Captions.balance)+": " + wrapBold(accountDetails.getBalance() + " "+accountDetails.getCurrency()), ContentMode.HTML));
        header.addComponent(new Label(bundle.getString(Captions.expireDate)+": " + createExpiryString(user.getExpiryDate()), ContentMode.HTML));
        String status = tradingEngine.isRunning() ? bundle.getString(Captions.active) : bundle.getString(Captions.notActive);
        header.addComponent(new Label(bundle.getString(Captions.botStatus)+": " + wrapBold(status), ContentMode.HTML));
        header.addComponent(createStartButton(tradingEngine, accountDetails));
        if (user.getCreated().equals(user.getExpiryDate())) {
            header.addComponent(createTrialButton(user));
        }
        Link supportLink = createSupportLink();
        header.addComponent(supportLink);
        header.setComponentAlignment(supportLink, Alignment.MIDDLE_RIGHT);
        return header;
    }

    private void updateTradingEngine(SessionTokenDiscoverer tokenDiscoverer, User user, TradingEngine tradingEngine) {
        tradingEngine.setMessageListener(createMessageListener());
        tradingEngine.setUser(user);
        tradingEngine.setTradingConfig(user.getTradingConfig());
        tradingEngine.setTokenDiscoverer(tokenDiscoverer);
    }

    private String createExpiryString(Date expiryDate) {
        String color = "green";
        if (System.currentTimeMillis() > expiryDate.getTime()) {
            color = "red";
        }
        return String.format("<font color=\"%s\"><strong>%s</strong></font>", color, expiryDate.toString());
    }

    private String wrapBold(String value) {
        return "<b>" + value + "</b>";
    }

    private MessageListener createMessageListener() {
        return message -> {
            VaadinSession session = getSession();
            if(session == null) {
                return;
            }
            TextArea eventsLog = (TextArea) session.getAttribute(EVENTS_LOG);
            access(() -> {
                eventsLog.setValue(eventsLog.getValue() + message + "\n");
                push();
            });
        };
    }

    private TextArea createLogArea() {
        TextArea textArea = new TextArea(bundle.getString(Captions.eventsLog));
        textArea.setSizeFull();
        textArea.setWidth("85%");
        textArea.setEnabled(false);
        textArea.setRows(10);
        return textArea;
    }

    private Component createTrialButton(User user) {
        return new Button(bundle.getString(Captions.activateTrial), (Button.ClickListener) event -> {
            user.setExpiryDate(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
            DBHelper.updateUser(user);
            Notification.show(bundle.getString(Captions.trialActivated), Notification.Type.TRAY_NOTIFICATION);
            UI.getCurrent().getPage().reload();
        });
    }

    private Button createStartButton(TradingEngine tradingEngine, AccountDetails accountDetails) {
        Button button = new Button(tradingEngine.isRunning() ? bundle.getString(Captions.stop) : bundle.getString(Captions.start));
        button.addClickListener((Button.ClickListener) event -> {
            LOG.debug(String.format("%s button clicked by '%s'", button.getCaption(), getCurrentUsername()));
            button.setEnabled(false);
            if (tradingEngine.isRunning()) {
                tradingEngine.stop();
                button.setCaption(bundle.getString(Captions.start));
                Notification.show(bundle.getString(Captions.botStopped), Notification.Type.TRAY_NOTIFICATION);
            } else {
                startTradingEngine(tradingEngine, accountDetails, button);
            }
            button.setEnabled(true);
        });
        return button;
    }

    private void startTradingEngine(TradingEngine tradingEngine, AccountDetails accountDetails, Button button) {
        if(accountDetails.getBalance() >= BetfairUtils.MIN_BALANCE) {
            try {
                tradingEngine.start();
                button.setCaption(bundle.getString(Captions.stop));
                Notification.show(bundle.getString(Captions.botStarted), Notification.Type.TRAY_NOTIFICATION);
            } catch (SubscriptionExpiredException e) {
                Notification.show(bundle.getString(Captions.subscriptionExpired), Notification.Type.WARNING_MESSAGE);
            }
        } else {
            Notification.show(bundle.getString(Captions.minBalance)+" = "+BetfairUtils.MIN_BALANCE, Notification.Type.WARNING_MESSAGE);
        }
    }

    private String getCurrentUsername() {
        User user = getCurrentUser();
        if(user != null) {
            return user.getUserName();
        }
        return null;
    }

    private User getCurrentUser() {
        VaadinSession session = getSession();
        Object user;
        if(session != null && (user = session.getAttribute(USER)) != null) {
            return ((User) user);
        }
        return null;
    }

    private TradingConfig getDefaultTradingConfig() {
        return new TradingConfig(10, 5, true, 3.0, 5.0, 20, 5);
    }
}
