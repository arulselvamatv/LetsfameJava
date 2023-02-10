package com.letsfame.serviceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.letsfame.bean.Subscriptions;
import com.letsfame.repository.WebHooksResponseRepository;
import com.letsfame.response.Response;
import com.letsfame.service.SubscriptionService;
import com.letsfame.service.webhookService;
import com.letsfame.webhook.Entity;
import com.letsfame.webhook.PaymentDetailsWebhook;
import com.letsfame.webhook.PaymentStatusByMember;
import com.razorpay.Invoice;
import com.razorpay.Plan;
import com.razorpay.RazorpayClient;

@Service
@Transactional
public class WebhookServiceImpl implements webhookService {

	@Autowired
	private WebHooksResponseRepository webHooksResponseRepository;

	@Value("${com.letsfame.serviceImpl.WebhookServiceImpl.username}")
	private String username;
	@Value("${com.letsfame.serviceImpl.WebhookServiceImpl.password}")
	private String password;
	@Value("${com.letsfame.serviceImpl.WebhookServiceImpl.url}")
	private String url;
	@Autowired
	private RazorpayClient razorpayClient;

	@Autowired
	private SubscriptionService subscriptionService;
	RestTemplate restTemplate = new RestTemplate();

	@Override
	public Response getpayment(PaymentDetailsWebhook notification) {

		Response response = new Response();
		if (!"payment.captured".equalsIgnoreCase(notification.getEvent().getEvent())) {
			return response;
		}

		PaymentDetailsWebhook savedData1 = new PaymentDetailsWebhook();
		PaymentStatusByMember paymentstatus = new PaymentStatusByMember();
		List<String> error = new ArrayList<>();

		System.out.println("webhook ::" + notification);
		try {

			// To get invoice details for find subscription ID
			Invoice invoice = razorpayClient.invoices
					.fetch(notification.getEvent().getPayload().getPayment().getEntity().getInvoice_id());
			System.out.println("invoice::"+invoice.get("subscription_id"));
			notification.setSubscriptionId(invoice.get("subscription_id"));

			// To save payments status
			savedData1 = webHooksResponseRepository.save(notification);

			// To share data to member API

			paymentstatus
					.setRazorCustomerId(savedData1.getEvent().getPayload().getPayment().getEntity().getCustomer_id());
			paymentstatus.setSubscriptionId(notification.getSubscriptionId());
			paymentstatus.setPaymentId(savedData1.getEvent().getPayload().getPayment().getEntity().getId());
//			paymentstatus.setSubscribedAt(savedData1.getEvent().getPayload().getPayment().getEntity().getCreated_at());

			// To get Member ID
			Subscriptions subscription = subscriptionService.findBySubscriptionsId(notification.getSubscriptionId());
			String memberId = subscription.getMemberId();

			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(username, password);
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			HttpEntity<PaymentStatusByMember> entity = new HttpEntity<PaymentStatusByMember>(paymentstatus, headers);
			String fullUrl = url + "/api/v1.0/member/" + memberId + "/subscription";
			System.out.println("Full URL::" + fullUrl);
			
			ResponseEntity<String> res = restTemplate.exchange(fullUrl, HttpMethod.PUT, entity, String.class);
			
			System.out.println("res:::"+res);

			response.setData(new JSONObject(res.getBody()).toMap());
			response.setMessage("Success");

		} catch (Exception e) {

			response.setMessage("Failed");
			error.add(e.getMessage());
//			response.setMessages(error);
			// response.getMessages().add(e.getMessage());
			System.out.println("Error :: createPlan :: Exception::" + ExceptionUtils.getStackTrace(e));

		}
		return response;
	}
}
