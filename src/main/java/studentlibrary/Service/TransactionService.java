package studentlibrary.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import studentlibrary.DTO.IssueBookRequestDto;
import studentlibrary.DTO.IssueBookResponseDto;
import studentlibrary.Entity.Book;
import studentlibrary.Entity.LibraryCard;
import studentlibrary.Entity.Transaction;
import studentlibrary.Enum.CardStatus;
import studentlibrary.Enum.TransactionStatus;
import studentlibrary.Repository.BookRepository;
import studentlibrary.Repository.CardRepository;
import studentlibrary.Repository.TransactionRepository;

@Service
public class TransactionService {

    @Autowired
    CardRepository cardRepository;

    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    BookRepository bookRepository;

    @Autowired
    private JavaMailSender emailSender;

    public IssueBookResponseDto issueBook(IssueBookRequestDto issueBookRequestDto) throws Exception {

        // Create Transaction Object
        Transaction transaction = new Transaction();
        transaction.setTransactionNumber(String.valueOf(UUID.randomUUID()));
        transaction.setIssueOperation(true);

        // 1 Step
        LibraryCard card;
        try{
            card = cardRepository.findById(issueBookRequestDto.getCardId()).get();
        }
        catch (Exception e){
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setMessage("Invalid card id");
            transactionRepository.save(transaction);
            throw new Exception("Invalid card id");
        }

        Book book;
        try{
            book = bookRepository.findById(issueBookRequestDto.getBookId()).get();
        }
        catch(Exception e){
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setMessage("Invalid Book Id");
            transactionRepository.save(transaction);
            throw new Exception("Invalid Book Id");
        }

        // both and card and book are valid
        transaction.setBook(book);
        transaction.setCard(card);

        if(card.getStatus()!= CardStatus.ACTIVATED){
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setMessage("Your card is not activated");
            transactionRepository.save(transaction);
            throw new Exception("Your card is not activated");
        }

        if(book.isIssued()==true){
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setMessage("Sorry! Book is already issued.");
            transactionRepository.save(transaction);
            throw new Exception("Sorry! Book is already issued.");
        }

        // I can issue the book
        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setMessage("Transaction was succesfull");

        book.setIssued(true);
        book.setCard(card);
        book.getTransaction().add(transaction);
        card.getTransactionList().add(transaction);
        card.getBooksIssued().add(book);

        cardRepository.save(card);  // will save book and tranaction also

        // Prepare Response Dto
        IssueBookResponseDto issueBookResponseDto = new IssueBookResponseDto();
        issueBookResponseDto.setTransanctionId(transaction.getTransactionNumber());
        issueBookResponseDto.setTransactionStatus(TransactionStatus.SUCCESS);
        issueBookResponseDto.setBookName(book.getTitle());

        // send an email
        String text = "Congrats !!." + card.getStudent().getName()+ "You have been issued "+book.getTitle()+" book.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("springtest139@gmail.com");
        message.setTo(card.getStudent().getEmail());
        message.setSubject("Issue Book Notification");
        message.setText(text);
        emailSender.send(message);

        return issueBookResponseDto;

    }

    public String getAllTxns(int cardId){
        List<Transaction> transactionList = transactionRepository.getAllSuccessfullTxnsWithCardNo(cardId);
        String ans = "";
        for(Transaction t: transactionList){
            ans += t.getTransactionNumber();
            ans += "\n";
        }

        return ans;
    }
}

